package com.aa.chatapp.feature.chat.domain.usecase

import com.aa.chatapp.feature.chat.domain.model.Message
import com.aa.chatapp.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMessagesUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    operator fun invoke(): Flow<List<Message>> = repository.observeMessages()
}
