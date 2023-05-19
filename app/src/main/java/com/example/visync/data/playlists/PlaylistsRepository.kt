package com.example.visync.data.playlists

interface PlaylistsRepository {

    fun getAllPlaylists(): List<Playlist>

    fun getPlaylist(id: Long): Playlist?

    fun addPlaylist(playlist: Playlist): Boolean
}