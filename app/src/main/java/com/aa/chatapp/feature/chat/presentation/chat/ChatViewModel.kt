package com.aa.chatapp.feature.chat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.aa.chatapp.core.datastore.UserPreferencesDataSource
import com.aa.chatapp.core.work.WorkConstants
import com.aa.chatapp.feature.chat.data.worker.SendMessageWorker
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
    private val workManager: WorkManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
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
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.OnInputChanged -> _state.update { it.copy(inputText = intent.text) }
            is ChatIntent.OnRemoveAttachment -> _state.update {
                it.copy(selectedAttachments = it.selectedAttachments.filter { a -> a.id != intent.attachmentId })
            }
            ChatIntent.OnSendClicked -> sendMessage()
            is ChatIntent.OnRetryMessage -> retryMessage(intent.messageId)
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
            enqueueWork(message.id)
            _state.update { it.copy(inputText = "", selectedAttachments = emptyList()) }
        }
    }

    private fun retryMessage(messageId: String) {
        viewModelScope.launch {
            retryMessageUseCase(messageId) // resets status to SENDING in Room
            enqueueWork(messageId)
        }
    }

    private fun enqueueWork(messageId: String) {
        val request = OneTimeWorkRequestBuilder<SendMessageWorker>()
            .setInputData(workDataOf(WorkConstants.KEY_MESSAGE_ID to messageId))
            .addTag(WorkConstants.TAG_SEND_MESSAGE)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            WorkConstants.uniqueWorkName(messageId),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
