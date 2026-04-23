package com.aa.chatapp.feature.chat.presentation.chat

sealed interface ChatIntent {
    data class OnInputChanged(val text: String) : ChatIntent
    data class OnImagesSelected(val uris: List<String>) : ChatIntent
    data class OnRemoveAttachment(val attachmentId: String) : ChatIntent
    data object OnSendClicked : ChatIntent
    data class OnRetryMessage(val messageId: String) : ChatIntent
}
