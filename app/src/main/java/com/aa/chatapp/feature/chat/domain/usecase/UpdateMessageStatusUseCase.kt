package com.aa.chatapp.feature.chat.domain.usecase

import com.aa.chatapp.feature.chat.domain.model.MessageStatus
import com.aa.chatapp.feature.chat.domain.repository.ChatRepository
import javax.inject.Inject

class UpdateMessageStatusUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    suspend operator fun invoke(
        messageId: String,
        status: MessageStatus,
        failedReason: String? = null,
    ) = repository.updateMessageStatus(messageId, status, failedReason)
}
