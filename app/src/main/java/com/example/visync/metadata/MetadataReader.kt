package com.example.visync.metadata

import android.net.Uri

interface MetadataReader {

    fun getVideoMetadataFromUri(contentUri: Uri): VideoMetadata?
}