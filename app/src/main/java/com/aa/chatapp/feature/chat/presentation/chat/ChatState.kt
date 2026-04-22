package com.aa.chatapp.feature.chat.presentation.chat

import com.aa.chatapp.feature.chat.domain.model.Attachment
import com.aa.chatapp.feature.chat.domain.model.Message

data class ChatState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val selectedAttachments: List<Attachment> = emptyList(),
    val currentUserId: String = "",
    val currentUserName: String = "",
    val currentAvatarUrl: String? = null,
)
