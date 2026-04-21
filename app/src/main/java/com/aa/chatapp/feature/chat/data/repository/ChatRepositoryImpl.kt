package com.aa.chatapp.feature.chat.data.repository

import com.aa.chatapp.feature.chat.data.local.dao.MessageDao
import com.aa.chatapp.feature.chat.data.mapper.toDomain
import com.aa.chatapp.feature.chat.data.mapper.toEntity
import com.aa.chatapp.feature.chat.domain.model.Message
import com.aa.chatapp.feature.chat.domain.model.MessageStatus
import com.aa.chatapp.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val dao: MessageDao,
) : ChatRepository {

    override fun observeMessages(): Flow<List<Message>> =
        dao.observeMessages().map { entities -> entities.map { it.toDomain() } }

    override suspend fun insertPendingMessage(message: Message) =
        dao.insertOrReplace(message.toEntity())

    override suspend fun updateMessageStatus(
        messageId: String,
        status: MessageStatus,
        failedReason: String?,
    ) = dao.updateMessageStatus(messageId, status.name, failedReason)

    override suspend fun retryMessage(messageId: String) =
        dao.resetMessageStatus(messageId, MessageStatus.SENDING.name)

    override suspend fun getMessageById(messageId: String): Message? =
        dao.getMessageById(messageId)?.toDomain()
}
