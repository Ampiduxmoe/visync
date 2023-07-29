package com.example.visync.data.videofolder

import android.net.Uri

data class Videofolder(
    val id: Long,
    val uri: Uri,
    val foldername: String,
    val includeChildren: Boolean,
    val playlistId: Long,
)
