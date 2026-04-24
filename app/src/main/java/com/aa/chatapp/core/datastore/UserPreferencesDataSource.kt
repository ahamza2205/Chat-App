package com.aa.chatapp.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val userId: Flow<String?> = dataStore.data.map { it[KEY_USER_ID] }
    val userName: Flow<String?> = dataStore.data.map { it[KEY_USER_NAME] }
    val avatarUrl: Flow<String?> = dataStore.data.map { it[KEY_AVATAR_URL] }
    val fcmToken: Flow<String?> = dataStore.data.map { it[KEY_FCM_TOKEN] }

    suspend fun saveUserIdentity(id: String, name: String, avatarUrl: String? = null) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = id
            prefs[KEY_USER_NAME] = name
            avatarUrl?.let { prefs[KEY_AVATAR_URL] = it }
        }
    }

    suspend fun getOrGenerateUserId(): String {
        val existing = dataStore.data.first()[KEY_USER_ID]
        if (existing != null) return existing
        val newId = UUID.randomUUID().toString()
        saveUserIdentity(newId, "User ${newId.take(4)}")
        return newId
    }

    suspend fun saveUserName(name: String) {
        dataStore.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun saveAvatarUrl(url: String) {
        dataStore.edit { it[KEY_AVATAR_URL] = url }
    }

    suspend fun saveFcmToken(token: String) {
        dataStore.edit { it[KEY_FCM_TOKEN] = token }
    }

    companion object {
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_AVATAR_URL = stringPreferencesKey("avatar_url")
        private val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")
    }
}
