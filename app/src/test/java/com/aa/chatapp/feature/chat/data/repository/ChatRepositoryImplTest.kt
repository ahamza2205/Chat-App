package com.aa.chatapp.feature.chat.data.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.aa.chatapp.core.work.WorkConstants
import com.aa.chatapp.feature.chat.data.local.dao.MessageDao
import com.aa.chatapp.feature.chat.data.local.entity.MessageEntity
import com.aa.chatapp.feature.chat.domain.model.Message
import com.aa.chatapp.feature.chat.domain.model.MessageStatus
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File

class ChatRepositoryImplTest {

    private lateinit var dao: MessageDao
    private lateinit var workManager: WorkManager
    private lateinit var context: Context
    private lateinit var repository: ChatRepositoryImpl

    @Before
    fun setup() {
        dao = mockk()
        workManager = mockk()
        context = mockk()
        repository = ChatRepositoryImpl(dao, workManager, context)
        
        val tempCacheDir = File(System.getProperty("java.io.tmpdir"), "test_cache").apply { mkdirs() }
        every { context.cacheDir } returns tempCacheDir
    }

    @Test
    fun `insertPendingMessage saves simple message and enqueues work`() = runTest {
        val message = Message(
            id = "msg1",
            senderId = "user1",
            senderName = "Alice",
            senderAvatarUrl = null,
            text = "Hello",
            attachments = emptyList(),
            status = MessageStatus.SENDING,
            createdAt = 1000L
        )

        coEvery { dao.insertOrReplace(any()) } just Runs
        every { workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<androidx.work.OneTimeWorkRequest>()) } returns mockk()

        repository.insertPendingMessage(message)

        coVerify { dao.insertOrReplace(match { it.id == "msg1" }) }
        verify { 
            workManager.enqueueUniqueWork(
                WorkConstants.uniqueWorkName("msg1"),
                ExistingWorkPolicy.KEEP,
                any<androidx.work.OneTimeWorkRequest>()
            ) 
        }
    }

    @Test
    fun `retryMessage resets status and enqueues REPLACE work`() = runTest {
        coEvery { dao.resetMessageStatus("msg1", MessageStatus.SENDING.name) } just Runs
        every { workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<androidx.work.OneTimeWorkRequest>()) } returns mockk()

        repository.retryMessage("msg1")

        coVerify { dao.resetMessageStatus("msg1", MessageStatus.SENDING.name) }
        verify { 
            workManager.enqueueUniqueWork(
                WorkConstants.uniqueWorkName("msg1"),
                ExistingWorkPolicy.REPLACE,
                any<androidx.work.OneTimeWorkRequest>()
            ) 
        }
    }

    @Test
    fun `cancelMessage updates status to FAILED and cancels work`() = runTest {
        coEvery { dao.updateMessageStatus("msg1", MessageStatus.FAILED.name, "Cancelled by user") } just Runs
        every { workManager.cancelUniqueWork(any<String>()) } returns mockk()

        repository.cancelMessage("msg1")

        coVerify { dao.updateMessageStatus("msg1", MessageStatus.FAILED.name, "Cancelled by user") }
        verify { workManager.cancelUniqueWork(WorkConstants.uniqueWorkName("msg1")) }
    }
}
