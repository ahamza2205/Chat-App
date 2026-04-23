package com.aa.chatapp.feature.chat.data.repository

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.aa.chatapp.core.network.supabaseClient
import com.aa.chatapp.core.work.WorkConstants
import com.aa.chatapp.feature.chat.data.local.dao.MessageDao
import com.aa.chatapp.feature.chat.data.mapper.toDomain
import com.aa.chatapp.feature.chat.data.mapper.toEntity
import com.aa.chatapp.feature.chat.data.worker.SendMessageWorker
import com.aa.chatapp.feature.chat.domain.model.Message
import com.aa.chatapp.feature.chat.domain.model.MessageStatus
import com.aa.chatapp.feature.chat.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val dao: MessageDao,
    private val workManager: WorkManager,
    @ApplicationContext private val context: Context,
) : ChatRepository {

    override fun observeMessages(): Flow<List<Message>> =
        dao.observeMessages().map { entities -> entities.map { it.toDomain() } }

    override suspend fun insertPendingMessage(message: Message) {
        val cachedAttachments = withContext(Dispatchers.IO) {
            message.attachments.map { attachment ->
                val localUri = attachment.localUri
                if (localUri != null && localUri.startsWith("content://")) {
                    try {
                        val uri = Uri.parse(localUri)
                        val dir = File(context.cacheDir, "chat_media").apply { mkdirs() }
                        val file = File(dir, "${UUID.randomUUID()}.jpg")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        attachment.copy(localUri = Uri.fromFile(file).toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        attachment
                    }
                } else {
                    attachment
                }
            }
        }
        val safeMessage = message.copy(attachments = cachedAttachments)
        dao.insertOrReplace(safeMessage.toEntity())
        enqueueWork(safeMessage.id, replace = false)
    }

    override suspend fun updateMessageStatus(
        messageId: String,
        status: MessageStatus,
        failedReason: String?,
    ) = dao.updateMessageStatus(messageId, status.name, failedReason)

    override suspend fun retryMessage(messageId: String) {
        dao.resetMessageStatus(messageId, MessageStatus.SENDING.name)
        enqueueWork(messageId, replace = true)
    }

    override suspend fun cancelMessage(messageId: String) {
        workManager.cancelUniqueWork(WorkConstants.uniqueWorkName(messageId))
        dao.updateMessageStatus(messageId, MessageStatus.FAILED.name, "Cancelled by user")
    }

    override suspend fun getMessageById(messageId: String): Message? =
        dao.getMessageById(messageId)?.toDomain()

    override suspend fun deleteForMe(messageId: String) {
        dao.hideMessage(messageId)
    }

    override suspend fun deleteForEveryone(messageId: String) {
        withContext(Dispatchers.IO) {
            supabaseClient.postgrest["messages"].update({
                set("is_deleted_for_everyone", true)
                set("text", null as String?)
                set("attachments", "[]")
            }) { filter { eq("id", messageId) } }
        }
        dao.softDeleteForEveryone(messageId)
    }

    private fun enqueueWork(messageId: String, replace: Boolean) {
        val request = OneTimeWorkRequestBuilder<SendMessageWorker>()
            .setInputData(workDataOf(WorkConstants.KEY_MESSAGE_ID to messageId))
            .addTag(WorkConstants.TAG_SEND_MESSAGE)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            WorkConstants.uniqueWorkName(messageId),
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
