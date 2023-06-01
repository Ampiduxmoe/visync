package com.example.visync.data.videofiles

import android.net.Uri

data class Videofile(
    val id: Long,
    val uri: Uri,
    val filename: String,
    val playlistId: Long,
)