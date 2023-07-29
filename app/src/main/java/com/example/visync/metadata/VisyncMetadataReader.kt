package com.example.visync.metadata

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File

class VisyncMetadataReader(
    private val app: Application
): MetadataReader {

    override fun isContentUriValid(contentUri: Uri): Boolean {
        if (contentUri.scheme != "content") {
            Log.w(
                VisyncMetadataReader::class.simpleName,
                "URI provided was not of scheme 'content' but ${contentUri.scheme}"
            )
            return false
        }
        val filepathCol = MediaStore.Video.VideoColumns.DATA
        return app.contentResolver
            .query(
                /* uri = */ contentUri,
                /* projection = */ arrayOf(filepathCol),
                /* selection = */ null,
                /* selectionArgs = */ null,
                /* sortOrder = */ null,
            )
            ?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use false
                }
                cursor.getString(0)?.let { filepath ->
                    val file = File(filepath)
                    return@use file.exists()
                } ?: throw NullPointerException("shit")
            } ?: false
    }

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
        return app.contentResolver
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

    override fun getFolderMetadata(folderUri: Uri): FolderMetadata? {
        if (folderUri.scheme != "content") {
            Log.w(
                VisyncMetadataReader::class.simpleName,
                "URI provided was not of scheme 'content' but ${folderUri.scheme}"
            )
            return null
        }
        val displayNameCol = OpenableColumns.DISPLAY_NAME
        val filename = app.contentResolver
            .query(
                /* uri = */ folderUri,
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
        return (filename ?: folderUri.lastPathSegment)?.let {
            FolderMetadata(
                foldername = it
            )
        }
    }

    override fun scanFolderForVideos(
        folderUri: Uri,
        recursiveSearch: Boolean
    ): Map<Uri, VideoMetadata?>? {
        if (folderUri.scheme != "content") {
            Log.w(
                VisyncMetadataReader::class.simpleName,
                "URI provided was not of scheme 'content' but ${folderUri.scheme}"
            )
            return null
        }

        val rootDir = DocumentFile.fromTreeUri(app, folderUri) ?: return null
        val allDirsToScan = when (recursiveSearch) {
            true -> listOf(rootDir) + listAllSubdirsRecursively(rootDir)
            false -> listOf(rootDir)
        }

        val allVideos = mutableListOf<DocumentFile>()
        for (dir in allDirsToScan) {
            allVideos += dir.listFiles().filter {
                it.isFile &&
                it.type?.startsWith("video") ?: false
            }
        }
        allVideos.forEach { Log.d(VisyncMetadataReader::class.simpleName, "URI=${it.uri}") }
        return allVideos.map { it.uri }.associateWith { getVideoMetadataFromUri(it) }
    }

    private fun listAllSubdirsRecursively(dir: DocumentFile): List<DocumentFile> {
        val subdirs = dir.listFiles().filter { it.isDirectory }
        val childrenSubdirs = subdirs.map { listAllSubdirsRecursively(it) }.flatten()
        return subdirs + childrenSubdirs
    }
}