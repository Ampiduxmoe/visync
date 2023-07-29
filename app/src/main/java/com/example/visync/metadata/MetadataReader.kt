package com.example.visync.metadata

import android.net.Uri

interface MetadataReader {

    fun isContentUriValid(contentUri: Uri): Boolean

    fun getVideoMetadataFromUri(contentUri: Uri): VideoMetadata?

    fun getFolderMetadata(folderUri: Uri): FolderMetadata?

    fun scanFolderForVideos(folderUri: Uri, recursiveSearch: Boolean): Map<Uri, VideoMetadata?>?

}