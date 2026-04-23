package com.aa.chatapp.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aa.chatapp.core.work.WorkConstants
import com.aa.chatapp.feature.chat.domain.repository.ChatRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: ChatRepository
    @Inject lateinit var notificationHelper: ChatNotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getStringExtra(WorkConstants.KEY_MESSAGE_ID) ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    WorkConstants.ACTION_CANCEL -> {
                        repository.cancelMessage(messageId)
                    }
                    WorkConstants.ACTION_RETRY -> {
                        notificationHelper.dismissFailedNotification(messageId)
                        repository.retryMessage(messageId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
