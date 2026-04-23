package com.aa.chatapp.feature.chat.data.worker

import android.content.Context
import android.net.Uri
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

        val pendingAttachments = entity.attachments.filter { it.localUri != null }

        if (pendingAttachments.size > 1) {
            setForeground(notificationHelper.createUploadForegroundInfo(messageId, 1, pendingAttachments.size))
        } else {
            setForeground(notificationHelper.createForegroundInfo(messageId))
        }

        return try {
            val uploaded = entity.attachments.mapIndexed { index, attachment ->
                val localUri = attachment.localUri ?: return@mapIndexed attachment

                if (pendingAttachments.size > 1) {
                    setForeground(
                        notificationHelper.createUploadForegroundInfo(messageId, index + 1, pendingAttachments.size)
                    )
                } else {
                    setForeground(notificationHelper.createForegroundInfo(messageId))
                }

                val bytes = applicationContext.contentResolver
                    .openInputStream(Uri.parse(localUri))?.use { it.readBytes() }
                    ?: throw IllegalStateException("Cannot read $localUri")

                val remotePath = "$messageId/${attachment.id}.jpg"
                val bucket = supabaseClient.storage["attachments"]
                bucket.upload(remotePath, bytes) { upsert = true }
                val remoteUrl = bucket.publicUrl(remotePath)
                attachment.copy(localUri = null, remoteUrl = remoteUrl)
            }

            val readyEntity = entity.copy(attachments = uploaded)
            supabaseClient.postgrest["messages"].insert(readyEntity.toRemote())
            dao.updateMessageStatus(messageId, MessageStatus.SENT.name, null)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                dao.updateMessageStatus(messageId, MessageStatus.FAILED.name, e.message)
                notificationHelper.showFailedNotification(messageId)
                Result.failure()
            }
        }
    }
}
