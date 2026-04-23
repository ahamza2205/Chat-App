package com.aa.chatapp.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.aa.chatapp.core.work.WorkConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL_ID = "chat_sending"

@Singleton
class ChatNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Sending messages", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    fun createForegroundInfo(messageId: String): ForegroundInfo =
        buildForegroundInfo("Sending message…", messageId)

    fun createUploadForegroundInfo(messageId: String, current: Int, total: Int): ForegroundInfo =
        buildForegroundInfo("Uploading image $current of $total…", messageId)

    fun showFailedNotification(messageId: String) {
        val retryIntent = Intent(WorkConstants.ACTION_RETRY).apply {
            setPackage(context.packageName)
            putExtra(WorkConstants.KEY_MESSAGE_ID, messageId)
        }
        val retryPending = PendingIntent.getBroadcast(
            context, messageId.hashCode(), retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Message failed to send")
            .setContentText("Tap Retry to try again")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, "Retry", retryPending)
            .build()

        val notificationId = 2000 + messageId.hashCode().and(0xFFF)
        manager.notify(notificationId, notification)
    }

    fun dismissFailedNotification(messageId: String) {
        val notificationId = 2000 + messageId.hashCode().and(0xFFF)
        manager.cancel(notificationId)
    }

    private fun buildForegroundInfo(title: String, messageId: String): ForegroundInfo {
        val cancelIntent = Intent(WorkConstants.ACTION_CANCEL).apply {
            setPackage(context.packageName)
            putExtra(WorkConstants.KEY_MESSAGE_ID, messageId)
        }
        val cancelPending = PendingIntent.getBroadcast(
            context, messageId.hashCode(), cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Cancel", cancelPending)
            .build()

        val notificationId = 1000 + messageId.hashCode().and(0xFFF)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }
}
