package com.aa.chatapp.feature.chat.domain.usecase

import com.aa.chatapp.feature.chat.domain.model.Message
import com.aa.chatapp.feature.chat.domain.repository.ChatRepository
import javax.inject.Inject

class InsertPendingMessageUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    suspend operator fun invoke(message: Message) =
        repository.insertPendingMessage(message)
}
