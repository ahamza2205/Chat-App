package com.aa.chatapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.aa.chatapp.core.datastore.UserPreferencesDataSource
import com.aa.chatapp.core.network.supabaseClient
import com.aa.chatapp.feature.chat.data.remote.SupabaseRealtimeDataSource
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@HiltAndroidApp
class ChatApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var realtimeDataSource: SupabaseRealtimeDataSource
    @Inject lateinit var userPrefs: UserPreferencesDataSource

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @Serializable
    private data class UserTokenRow(val user_id: String, val fcm_token: String)

    override fun onCreate() {
        super.onCreate()
        realtimeDataSource.start(appScope)
        fetchAndStoreFcmToken()
        startTokenSync()
    }

    private fun fetchAndStoreFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            appScope.launch { userPrefs.saveFcmToken(token) }
        }
    }

    private fun startTokenSync() {
        combine(userPrefs.userId, userPrefs.fcmToken) { id, token -> id to token }
            .onEach { (userId, fcmToken) ->
                if (!userId.isNullOrBlank() && !fcmToken.isNullOrBlank()) {
                    runCatching {
                        supabaseClient.from("user_tokens").upsert(
                            UserTokenRow(user_id = userId, fcm_token = fcmToken)
                        )
                    }
                }
            }
            .launchIn(appScope)
    }
}
