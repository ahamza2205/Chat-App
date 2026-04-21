package com.aa.chatapp.core.database

import androidx.room.TypeConverter
import com.aa.chatapp.feature.chat.domain.model.Attachment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromAttachments(value: String): List<Attachment> = json.decodeFromString(value)

    @TypeConverter
    fun toAttachments(attachments: List<Attachment>): String = json.encodeToString(attachments)
}
