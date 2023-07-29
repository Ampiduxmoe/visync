package com.example.visync.data.videofiles

import android.net.Uri
import com.example.visync.data.playlists.Playlist
import kotlinx.coroutines.flow.Flow

interface VideofilesRepository {

    val videofiles: Flow<List<Videofile>>

    fun tryAddVideofilesToPlaylist(playlist: Playlist, vararg videofiles: Videofile): List<Long>

    fun removeVideofiles(vararg videofiles: Videofile)

    fun selectExistingFromUris(vararg uris: Uri): List<Uri>
}