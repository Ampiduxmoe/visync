package com.example.visync.metadata

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

class VisyncMetadataReader(
    private val context: Context
): MetadataReader {

    override fun getVideoMetadataFromUri(contentUri: Uri): VideoMetadata? {
        if (contentUri.scheme != "content") {
            Log.w(
                VisyncMetadataReader::class.simpleName,
                "URI provided was not of scheme 'content' but ${contentUri.scheme}"
            )
            return null
        }
        val displayNameCol = MediaStore.Video.VideoColumns.DISPLAY_NAME
        val durationCol = MediaStore.Video.VideoColumns.DURATION
        return context.contentResolver
            .query(
                /* uri = */ contentUri,
                /* projection = */ arrayOf(displayNameCol, durationCol),
                /* selection = */ null,
                /* selectionArgs = */ null,
                /* sortOrder = */ null,
            )
            ?.use { cursor ->
                val displayNameIndex = cursor.getColumnIndex(displayNameCol)
                val durationIndex = cursor.getColumnIndex(durationCol)

                cursor.moveToFirst()
                val displayName = cursor.getString(displayNameIndex)
                val duration = cursor.getLong(durationIndex)

                VideoMetadata(
                    filename = displayName ?: contentUri.lastPathSegment ?: return null,
                    duration = duration
                )
            }
    }
}