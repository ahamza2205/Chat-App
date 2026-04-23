package com.aa.chatapp.feature.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ReplyPreview(
    val originalMessageId: String,
    val senderName: String,
    val textPreview: String?,
    val isMedia: Boolean,
)
