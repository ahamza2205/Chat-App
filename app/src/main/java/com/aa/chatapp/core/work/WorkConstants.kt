package com.aa.chatapp.core.work

object WorkConstants {
    const val KEY_MESSAGE_ID = "message_id"
    const val TAG_SEND_MESSAGE = "send_message"

    const val ACTION_CANCEL = "com.aa.chatapp.ACTION_CANCEL_SEND"
    const val ACTION_RETRY = "com.aa.chatapp.ACTION_RETRY_SEND"

    fun uniqueWorkName(messageId: String) = "send_$messageId"
}
