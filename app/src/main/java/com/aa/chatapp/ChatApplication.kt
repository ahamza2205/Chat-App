package com.aa.chatapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.aa.chatapp.core.coroutines.CoroutineContextProvider
import com.aa.chatapp.core.datastore.UserPreferencesDataSource
import com.aa.chatapp.core.network.supabaseClient
import com.aa.chatapp.feature.chat.data.remote.SupabaseRealtimeDataSource
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import io.github.jan.supabase.postgrest.from
import android.util.Log
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import javax.inject.Inject

@HiltAndroidApp
class ChatApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var realtimeDataSource: SupabaseRealtimeDataSource
    @Inject lateinit var userPrefs: UserPreferencesDataSource
    @Inject lateinit var contextProvider: CoroutineContextProvider

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e("ChatApplication", "Unhandled coroutine exception in appScope", exception)
    }

    private val appScope by lazy {
        CoroutineScope(SupervisorJob() + contextProvider.io + exceptionHandler)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @Serializable
    private data class UserTokenRow(val user_id: String, val fcm_token: String)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { runCatching { userPrefs.getOrGenerateUserId() } }
        realtimeDataSource.start(appScope)
        fetchAndStoreFcmToken()
        startTokenSync()
    }

    private fun fetchAndStoreFcmToken() {
        appScope.launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                userPrefs.saveFcmToken(token)
            }.onFailure { exception ->
                Log.e("ChatApplication", "Failed to fetch and store FCM token", exception)
            }
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

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            val exception = task.exception
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(exception ?: Exception("Task failed"))
            }
        }
    }
}
