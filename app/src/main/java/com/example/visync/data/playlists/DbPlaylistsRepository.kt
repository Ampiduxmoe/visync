package com.example.visync.data.playlists

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DbPlaylistsRepository @Inject constructor(
    private val playlistsDao: PlaylistDao
) : PlaylistsRepository {

    override val playlists = playlistsDao.getAll()


    override fun getPlaylistWithVideofiles(id: Long): Flow<PlaylistWithVideofiles?> {
        return playlistsDao.getByIdWithVideofiles(id)
    }

    override fun tryAddPlaylist(playlist: Playlist) {
        playlistsDao.insertNewOnly(playlist)
    }
}