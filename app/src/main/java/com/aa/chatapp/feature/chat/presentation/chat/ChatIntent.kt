package com.aa.chatapp.feature.chat.presentation.chat

import com.aa.chatapp.feature.chat.domain.model.Attachment
import com.aa.chatapp.feature.chat.domain.model.ReplyPreview

sealed interface ChatIntent {
    data class OnInputChanged(val text: String) : ChatIntent
    data class OnImagesSelected(val uris: List<String>) : ChatIntent
    data class OnRemoveAttachment(val attachmentId: String) : ChatIntent
    data class OnVoiceNoteReady(val attachment: Attachment) : ChatIntent
    data object OnSendClicked : ChatIntent
    data class OnRetryMessage(val messageId: String) : ChatIntent
    data class OnReplyToMessage(val reply: ReplyPreview) : ChatIntent
    data object OnClearReply : ChatIntent
}
