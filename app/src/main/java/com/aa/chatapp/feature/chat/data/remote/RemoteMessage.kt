package com.aa.chatapp.feature.chat.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteMessage(
    @SerialName("id")           val id: String,
    @SerialName("sender_id")    val senderId: String,
    @SerialName("sender_name")  val senderName: String,
    @SerialName("sender_avatar_url") val senderAvatarUrl: String? = null,
    @SerialName("text")         val text: String? = null,
    @SerialName("attachments")  val attachments: String = "[]",
    @SerialName("status")       val status: String = "SENT",
    @SerialName("created_at")   val createdAt: Long,
)
