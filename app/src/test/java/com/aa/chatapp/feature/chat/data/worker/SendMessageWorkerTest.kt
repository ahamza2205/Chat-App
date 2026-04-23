package com.aa.chatapp.feature.chat.data.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.aa.chatapp.core.notifications.ChatNotificationHelper
import com.aa.chatapp.core.work.WorkConstants
import com.aa.chatapp.feature.chat.data.local.dao.MessageDao
import com.aa.chatapp.feature.chat.data.local.entity.MessageEntity
import com.aa.chatapp.feature.chat.domain.model.MessageStatus
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SendMessageWorkerTest {

    private lateinit var context: Context
    private lateinit var params: WorkerParameters
    private lateinit var dao: MessageDao
    private lateinit var notificationHelper: ChatNotificationHelper
    private lateinit var worker: SendMessageWorker

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        params = mockk(relaxed = true)
        dao = mockk(relaxed = true)
        notificationHelper = mockk(relaxed = true)
    }

    private fun buildWorker(messageId: String?, runAttemptCount: Int = 0): SendMessageWorker {
        val dataBuilder = Data.Builder()
        if (messageId != null) {
            dataBuilder.putString(WorkConstants.KEY_MESSAGE_ID, messageId)
        }
        every { params.inputData } returns dataBuilder.build()
        every { params.runAttemptCount } returns runAttemptCount
        
        val worker = spyk(SendMessageWorker(context, params, dao, notificationHelper))
        coEvery { worker.setForeground(any()) } just Runs
        return worker
    }

    @Test
    fun `doWork returns failure when messageId is missing`() = runTest {
        worker = buildWorker(messageId = null)
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `doWork returns failure when message entity is missing in DB`() = runTest {
        coEvery { dao.getMessageById("msg1") } returns null
        worker = buildWorker(messageId = "msg1")

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `doWork returns retry on transient exact exception under 3 attempts`() = runTest {
        val dummyEntity = MessageEntity(
            id = "msg1",
            senderId = "s1",
            senderName = "tester",
            senderAvatarUrl = null,
            text = "txt",
            attachments = listOf(com.aa.chatapp.feature.chat.domain.model.Attachment("1", "file://test", null, "image/jpeg")),
            status = "SENDING",
            createdAt = 10L,
            failedReason = null
        )
        coEvery { dao.getMessageById("msg1") } returns dummyEntity
        
        // Context is mocked, so openInputStream will return null and throw IllegalStateException inside try-block
        
        worker = buildWorker(messageId = "msg1", runAttemptCount = 0)
        
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `doWork updates status to FAILED and fails when over 3 attempts`() = runTest {
        val dummyEntity = MessageEntity(
            id = "msg1",
            senderId = "s1",
            senderName = "tester",
            senderAvatarUrl = null,
            text = "txt",
            attachments = listOf(com.aa.chatapp.feature.chat.domain.model.Attachment("1", "file://test", null, "image/jpeg")),
            status = "SENDING",
            createdAt = 10L,
            failedReason = null
        )
        coEvery { dao.getMessageById("msg1") } returns dummyEntity
        coEvery { dao.updateMessageStatus("msg1", MessageStatus.FAILED.name, any()) } just Runs
        
        // Simulating failure by allowing the natural exception (e.g. unmocked Supabase access) to occur
        worker = buildWorker(messageId = "msg1", runAttemptCount = 3) // Attempt 3 -> fails forever

        val result = worker.doWork()

        coVerify { dao.updateMessageStatus("msg1", MessageStatus.FAILED.name, any()) }
        coVerify { notificationHelper.showFailedNotification("msg1") }
        assertEquals(ListenableWorker.Result.failure(), result)
    }
}
