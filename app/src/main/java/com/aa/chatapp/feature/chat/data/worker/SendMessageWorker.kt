package com.aa.chatapp.feature.chat.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aa.chatapp.core.notifications.ChatNotificationHelper
import com.aa.chatapp.core.work.WorkConstants
import com.aa.chatapp.feature.chat.data.local.dao.MessageDao
import com.aa.chatapp.feature.chat.domain.model.MessageStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

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

        dao.getMessageById(messageId) ?: return Result.failure()

        setForeground(notificationHelper.createForegroundInfo())

        return try {
            // TODO (Phase 6): Replace with Supabase message insert + attachment uploads.
            delay(1_500L)

            dao.updateMessageStatus(messageId, MessageStatus.SENT.name, null)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                dao.updateMessageStatus(messageId, MessageStatus.FAILED.name, e.message)
                Result.failure()
            }
        }
    }
}
