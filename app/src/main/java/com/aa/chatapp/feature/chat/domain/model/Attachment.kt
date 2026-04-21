package com.aa.chatapp.feature.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val id: String,
    /** Non-null while the file is only on-device; null after a successful upload. */
    val localUri: String? = null,
    /** Non-null once the file is stored in Supabase Storage. */
    val remoteUrl: String? = null,
    val mimeType: String,
    val fileName: String? = null,
)
