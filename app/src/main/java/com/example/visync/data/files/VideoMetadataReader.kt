package com.example.visync.data.files

import android.net.Uri

interface VideoMetadataReader {

    fun getMetadataFromUri(contentUri: Uri): VideoMetadata?
}