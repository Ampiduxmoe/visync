package com.example.visync.data.playlists

import kotlinx.coroutines.flow.Flow

interface PlaylistsRepository {

    val playlists: Flow<List<Playlist>>

    fun getPlaylistWithVideofiles(id: Long): Flow<PlaylistWithVideofiles?>

    /** Adds new playlists if its id doesn't conflict with other playlists. */
    fun tryAddPlaylist(playlist: Playlist)
}