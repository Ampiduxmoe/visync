package com.example.visync.data.videofiles

import android.net.Uri
import com.example.visync.metadata.VideoMetadata

data class Videofile(
    val uri: Uri,
    val metadata: VideoMetadata,
) {
    fun equalsByUri(other: Videofile): Boolean = uri == other.uri
    fun equalsByMetadata(other: Videofile): Boolean = metadata == other.metadata
}