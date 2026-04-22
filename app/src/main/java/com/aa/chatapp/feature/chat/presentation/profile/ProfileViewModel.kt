package com.aa.chatapp.feature.chat.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aa.chatapp.core.datastore.UserPreferencesDataSource
import com.aa.chatapp.core.network.supabaseClient
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val name: String = "",
    val avatarUrl: String? = null,
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPrefs: UserPreferencesDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    name = userPrefs.userName.first().orEmpty(),
                    avatarUrl = userPrefs.avatarUrl.first(),
                )
            }
        }
    }

    fun onNameChanged(name: String) = _state.update { it.copy(name = name, savedSuccess = false) }

    fun onAvatarPicked(uri: Uri, getBytes: suspend (Uri) -> ByteArray?) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, savedSuccess = false) }
            try {
                val bytes = getBytes(uri) ?: return@launch
                val userId = userPrefs.userId.first() ?: return@launch
                val remotePath = "avatars/$userId.jpg"
                supabaseClient.storage["attachments"].upload(remotePath, bytes) { upsert = true }
                val remoteUrl = supabaseClient.storage["attachments"].publicUrl(remotePath)
                userPrefs.saveAvatarUrl(remoteUrl)
                _state.update { it.copy(avatarUrl = remoteUrl, isSaving = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "Avatar upload failed") }
            }
        }
    }

    fun onSave() {
        val name = _state.value.name.trim()
        if (name.isBlank()) {
            _state.update { it.copy(error = "Name cannot be empty") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            userPrefs.saveUserName(name)
            _state.update { it.copy(isSaving = false, savedSuccess = true) }
        }
    }
}
