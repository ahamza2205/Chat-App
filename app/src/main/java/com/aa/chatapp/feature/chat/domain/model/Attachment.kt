package com.aa.chatapp.feature.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val id: String,
    val localUri: String? = null,
    val remoteUrl: String? = null,
    val mimeType: String,
    val fileName: String? = null,
    val durationMs: Long? = null,
    val waveform: List<Float>? = null,
)
