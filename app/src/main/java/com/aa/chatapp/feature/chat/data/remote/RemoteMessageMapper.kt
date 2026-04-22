package com.aa.chatapp.feature.chat.data.remote

import com.aa.chatapp.feature.chat.data.local.entity.MessageEntity
import com.aa.chatapp.feature.chat.domain.model.Attachment
import com.aa.chatapp.feature.chat.domain.model.MessageStatus
import kotlinx.serialization.encodeToString

fun MessageEntity.toRemote(): RemoteMessage = RemoteMessage(
    id = id,
    senderId = senderId,
    senderName = senderName,
    senderAvatarUrl = senderAvatarUrl,
    text = text,
    attachments = remoteJson.encodeToString(attachments),
    status = MessageStatus.SENT.name,
    createdAt = createdAt,
)

fun RemoteMessage.toEntity(): MessageEntity = MessageEntity(
    id = id,
    senderId = senderId,
    senderName = senderName,
    senderAvatarUrl = senderAvatarUrl,
    text = text,
    attachments = runCatching {
        remoteJson.decodeFromString<List<Attachment>>(attachments)
    }.getOrDefault(emptyList()),
    status = status,
    createdAt = createdAt,
    failedReason = null,
)
