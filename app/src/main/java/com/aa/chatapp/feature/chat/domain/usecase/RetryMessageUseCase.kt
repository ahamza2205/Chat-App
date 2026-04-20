package com.aa.chatapp.feature.chat.domain.usecase

import com.aa.chatapp.feature.chat.domain.repository.ChatRepository
import javax.inject.Inject

class RetryMessageUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    suspend operator fun invoke(messageId: String) =
        repository.retryMessage(messageId)
}
