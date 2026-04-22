package com.aa.chatapp.feature.chat.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aa.chatapp.core.network.supabaseClient
import com.aa.chatapp.core.notifications.ChatNotificationHelper
import com.aa.chatapp.core.work.WorkConstants
import com.aa.chatapp.feature.chat.data.local.dao.MessageDao
import com.aa.chatapp.feature.chat.data.remote.toRemote
import com.aa.chatapp.feature.chat.domain.model.MessageStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException

@HiltWorker
class SendMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: MessageDao,
    private val notificationHelper: ChatNotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val messageId = inputData.getString(WorkConstants.KEY_MESSAGE_ID)
            ?: return Result.failure()

        val entity = dao.getMessageById(messageId) ?: return Result.failure()

        setForeground(notificationHelper.createForegroundInfo())

        return try {
            val uploaded = entity.attachments.mapNotNull { attachment ->
                val localPath = attachment.localUri ?: return@mapNotNull null
                val remotePath = "$messageId/${localPath.substringAfterLast('/')}"
                val remoteUrl = supabaseClient.storage["attachments"].let { bucket ->
                    bucket.upload(remotePath, java.io.File(localPath).readBytes()) { upsert = true }
                    bucket.publicUrl(remotePath)
                }
                attachment.copy(localUri = null, remoteUrl = remoteUrl)
            }

            val readyEntity = if (uploaded.isNotEmpty()) entity.copy(attachments = uploaded) else entity
            supabaseClient.postgrest["messages"].insert(readyEntity.toRemote())
            dao.updateMessageStatus(messageId, MessageStatus.SENT.name, null)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry()
            else {
                dao.updateMessageStatus(messageId, MessageStatus.FAILED.name, e.message)
                Result.failure()
            }
        }
    }
}
