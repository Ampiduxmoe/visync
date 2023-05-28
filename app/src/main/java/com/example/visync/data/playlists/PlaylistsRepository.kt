package com.example.visync.data.playlists

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface PlaylistsRepository {

    val playlists: StateFlow<List<Playlist>>

    fun getPlaylist(id: Long): Playlist?

    fun addPlaylist(playlist: Playlist): Boolean
}