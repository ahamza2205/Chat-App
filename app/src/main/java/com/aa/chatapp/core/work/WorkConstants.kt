package com.aa.chatapp.core.work

object WorkConstants {
    const val KEY_MESSAGE_ID = "message_id"
    const val TAG_SEND_MESSAGE = "send_message"

    fun uniqueWorkName(messageId: String) = "send_$messageId"
}
