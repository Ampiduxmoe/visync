package com.example.visync.data.files

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

class VisyncVideoMetadataReader(
    private val app: Application
): VideoMetadataReader {

    override fun getMetadataFromUri(contentUri: Uri): VideoMetadata? {
        Log.d("VisyncVideoMetadataReader", "contentUri is $contentUri")
        if (contentUri.scheme != "content") {
            return null
        }
        val displayNameCol = MediaStore.Video.VideoColumns.DISPLAY_NAME
        val filename = app.contentResolver
            .query(
                /* uri = */ contentUri,
                /* projection = */ arrayOf(displayNameCol),
                /* selection = */ null,
                /* selectionArgs = */ null,
                /* sortOrder = */ null,
            )
            ?.use { cursor ->
                val index = cursor.getColumnIndex(displayNameCol)
                cursor.moveToFirst()
                cursor.getString(index)
            }
        return filename?.let { fullFilename ->
            Log.d("VisyncVideoMetadataReader", "fullFilename is $fullFilename")
            VideoMetadata(
                filename = Uri.parse(fullFilename).lastPathSegment ?: return null
            )
        }
    }
}