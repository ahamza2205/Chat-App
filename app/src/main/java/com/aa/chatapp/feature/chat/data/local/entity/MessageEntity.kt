package com.aa.chatapp.feature.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aa.chatapp.feature.chat.domain.model.Attachment
import com.aa.chatapp.feature.chat.domain.model.ReplyPreview

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String?,
    val text: String?,
    val attachments: List<Attachment>,
    val status: String,
    val createdAt: Long,
    val failedReason: String?,
    val replyPreview: ReplyPreview? = null,
    val isDeletedForEveryone: Boolean = false,
    val hiddenForMe: Boolean = false,
)
