package com.aa.chatapp.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val userId: Flow<String?> = dataStore.data.map { it[KEY_USER_ID] }
    val userName: Flow<String?> = dataStore.data.map { it[KEY_USER_NAME] }
    val avatarUrl: Flow<String?> = dataStore.data.map { it[KEY_AVATAR_URL] }

    suspend fun saveUserIdentity(id: String, name: String, avatarUrl: String? = null) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = id
            prefs[KEY_USER_NAME] = name
            avatarUrl?.let { prefs[KEY_AVATAR_URL] = it }
        }
    }

    companion object {
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_AVATAR_URL = stringPreferencesKey("avatar_url")
    }
}
