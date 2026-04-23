package com.aa.chatapp.core.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.aa.chatapp.MainActivity
import com.aa.chatapp.core.datastore.UserPreferencesDataSource
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ChatMessagingService : FirebaseMessagingService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MessagingEntryPoint {
        fun userPrefs(): UserPreferencesDataSource
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val userPrefs: UserPreferencesDataSource by lazy {
        EntryPointAccessors.fromApplication(applicationContext, MessagingEntryPoint::class.java)
            .userPrefs()
    }

    override fun onNewToken(token: String) {
        serviceScope.launch { userPrefs.saveFcmToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val senderName = data["sender_name"] ?: return
        val text = data["text"]?.takeIf { it.isNotBlank() }

        val preview = text?.take(80) ?: "New message"
        showNotification(senderName, preview)
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "New messages", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(tapPending)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "new_messages"
    }
}
