package com.aa.chatapp.feature.chat.presentation.chat

sealed interface ChatEffect {
    data class ShowSnackbar(val message: String) : ChatEffect
}
