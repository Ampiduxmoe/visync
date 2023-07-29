package com.example.visync.metadata

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
data class VideoMetadata(
    val filename: String,
    val duration: Long,
)
