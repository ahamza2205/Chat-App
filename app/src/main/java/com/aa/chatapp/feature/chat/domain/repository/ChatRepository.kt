package com.aa.chatapp.feature.chat.domain.repository

import com.aa.chatapp.feature.chat.domain.model.Message
import com.aa.chatapp.feature.chat.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    fun observeMessages(): Flow<List<Message>>

    suspend fun insertPendingMessage(message: Message)

    suspend fun updateMessageStatus(
        messageId: String,
        status: MessageStatus,
        failedReason: String? = null,
    )

    suspend fun retryMessage(messageId: String)

    suspend fun getMessageById(messageId: String): Message?

    suspend fun cancelMessage(messageId: String)

    suspend fun deleteForMe(messageId: String)

    suspend fun deleteForEveryone(messageId: String)

    suspend fun updateUserProfile(userId: String, name: String, avatarUrl: String?)

    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): String
}
