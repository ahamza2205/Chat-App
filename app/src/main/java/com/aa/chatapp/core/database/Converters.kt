package com.aa.chatapp.core.database

import androidx.room.TypeConverter
import com.aa.chatapp.feature.chat.domain.model.Attachment
import com.aa.chatapp.feature.chat.domain.model.ReplyPreview
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromAttachments(value: String): List<Attachment> = json.decodeFromString(value)

    @TypeConverter
    fun toAttachments(attachments: List<Attachment>): String = json.encodeToString(attachments)

    @TypeConverter
    fun fromReplyPreview(value: String?): ReplyPreview? =
        value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun toReplyPreview(reply: ReplyPreview?): String? =
        reply?.let { json.encodeToString(it) }
}

