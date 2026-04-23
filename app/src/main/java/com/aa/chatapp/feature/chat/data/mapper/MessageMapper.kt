package com.aa.chatapp.feature.chat.data.mapper

import com.aa.chatapp.feature.chat.data.local.entity.MessageEntity
import com.aa.chatapp.feature.chat.domain.model.Message
import com.aa.chatapp.feature.chat.domain.model.MessageStatus

fun MessageEntity.toDomain(): Message = Message(
    id = id,
    senderId = senderId,
    senderName = senderName,
    senderAvatarUrl = senderAvatarUrl,
    text = text,
    attachments = attachments,
    status = MessageStatus.valueOf(status),
    createdAt = createdAt,
    failedReason = failedReason,
    replyPreview = replyPreview,
    isDeletedForEveryone = isDeletedForEveryone,
)

fun Message.toEntity(): MessageEntity = MessageEntity(
    id = id,
    senderId = senderId,
    senderName = senderName,
    senderAvatarUrl = senderAvatarUrl,
    text = text,
    attachments = attachments,
    status = status.name,
    createdAt = createdAt,
    failedReason = failedReason,
    replyPreview = replyPreview,
    isDeletedForEveryone = isDeletedForEveryone,
)
