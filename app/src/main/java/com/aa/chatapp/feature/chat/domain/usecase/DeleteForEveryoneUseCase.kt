package com.aa.chatapp.feature.chat.domain.usecase

import com.aa.chatapp.feature.chat.domain.repository.ChatRepository
import javax.inject.Inject

class DeleteForEveryoneUseCase @Inject constructor(private val repo: ChatRepository) {
    suspend operator fun invoke(messageId: String) = repo.deleteForEveryone(messageId)
}
