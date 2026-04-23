package com.aa.chatapp.feature.chat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aa.chatapp.core.datastore.UserPreferencesDataSource
import com.aa.chatapp.feature.chat.domain.model.Attachment
import com.aa.chatapp.feature.chat.domain.model.Message
import com.aa.chatapp.feature.chat.domain.model.MessageStatus
import com.aa.chatapp.feature.chat.domain.usecase.InsertPendingMessageUseCase
import com.aa.chatapp.feature.chat.domain.usecase.ObserveMessagesUseCase
import com.aa.chatapp.feature.chat.domain.usecase.RetryMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val observeMessages: ObserveMessagesUseCase,
    private val insertPendingMessage: InsertPendingMessageUseCase,
    private val retryMessageUseCase: RetryMessageUseCase,
    private val userPrefs: UserPreferencesDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())

    companion object {
        private const val MAX_ATTACHMENTS = 10
    }
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _effects = Channel<ChatEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (userPrefs.userId.first() == null) {
                val id = UUID.randomUUID().toString()
                userPrefs.saveUserIdentity(id = id, name = "User ${id.take(4)}")
            }
        }
        viewModelScope.launch {
            observeMessages().collect { messages ->
                _state.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            userPrefs.userId.collect { id ->
                _state.update { it.copy(currentUserId = id.orEmpty()) }
            }
        }
        viewModelScope.launch {
            userPrefs.userName.collect { name ->
                _state.update { it.copy(currentUserName = name.orEmpty()) }
            }
        }
        viewModelScope.launch {
            userPrefs.avatarUrl.collect { url ->
                _state.update { it.copy(currentAvatarUrl = url) }
            }
        }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.OnInputChanged -> _state.update { it.copy(inputText = intent.text) }
            is ChatIntent.OnImagesSelected -> addImages(intent.uris)
            is ChatIntent.OnRemoveAttachment -> _state.update {
                it.copy(selectedAttachments = it.selectedAttachments.filter { a -> a.id != intent.attachmentId })
            }
            ChatIntent.OnSendClicked -> sendMessage()
            is ChatIntent.OnRetryMessage -> retryMessage(intent.messageId)
        }
    }

    private fun addImages(uris: List<String>) {
        _state.update { current ->
            val existing = current.selectedAttachments
            val remaining = (MAX_ATTACHMENTS - existing.size).coerceAtLeast(0)
            val newAttachments = uris.take(remaining).map { uri ->
                Attachment(id = UUID.randomUUID().toString(), localUri = uri, mimeType = "image/jpeg")
            }
            if (uris.size > remaining) {
                _effects.trySend(ChatEffect.ShowSnackbar("Max $MAX_ATTACHMENTS images allowed"))
            }
            current.copy(selectedAttachments = existing + newAttachments)
        }
    }

    private fun sendMessage() {
        val current = _state.value
        if (current.inputText.isBlank() && current.selectedAttachments.isEmpty()) {
            _effects.trySend(ChatEffect.ShowSnackbar("Message cannot be empty"))
            return
        }
        viewModelScope.launch {
            val userId = userPrefs.userId.first() ?: return@launch
            val message = Message(
                id = UUID.randomUUID().toString(),
                senderId = userId,
                senderName = userPrefs.userName.first() ?: "Unknown",
                senderAvatarUrl = userPrefs.avatarUrl.first(),
                text = current.inputText.trim().takeIf { it.isNotBlank() },
                attachments = current.selectedAttachments,
                status = MessageStatus.SENDING,
                createdAt = System.currentTimeMillis(),
            )
                insertPendingMessage(message)
            _state.update { it.copy(inputText = "", selectedAttachments = emptyList()) }
        }
    }

    private fun retryMessage(messageId: String) {
        viewModelScope.launch {
            retryMessageUseCase(messageId) // resets status to SENDING in Room and enqueues worker
        }
    }
}
