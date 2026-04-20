package com.aa.chatapp.feature.chat.domain.model

data class Message(
    /** Client-generated UUID — enables optimistic UI and realtime deduplication. */
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String? = null,
    val text: String? = null,
    val attachments: List<Attachment> = emptyList(),
    val status: MessageStatus,
    val createdAt: Long,
    val failedReason: String? = null,
)
