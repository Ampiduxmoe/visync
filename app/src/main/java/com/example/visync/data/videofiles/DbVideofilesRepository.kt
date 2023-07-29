package com.example.visync.data.videofiles

import android.net.Uri
import com.example.visync.data.playlists.Playlist
import com.example.visync.data.relations.PlaylistVideofileCrossRef
import javax.inject.Inject

class DbVideofilesRepository @Inject constructor(
    private val videofileDao: VideofileDao,
) : VideofilesRepository {

    override val videofiles = videofileDao.getAll()

    override fun tryAddVideofilesToPlaylist(
        playlist: Playlist,
        vararg videofiles: Videofile
    ): List<Long> {
        val playlistId = playlist.playlistId
        val insertionResultIds = videofileDao.insertNewOnly(*videofiles)
        val onlySuccessInsertIds = insertionResultIds.filter { it != -1L }
        val maxPositionInPlaylist = videofileDao.getMaxPositionInPlaylist(playlistId)
        val crossRefs = onlySuccessInsertIds.mapIndexed { offset, videofileId ->
            PlaylistVideofileCrossRef(
                playlistId = playlistId,
                videofileId = videofileId,
                position = maxPositionInPlaylist + offset + 1
            )
        }.toTypedArray()
        videofileDao.addPlaylistCrossRefs(*crossRefs)
        return insertionResultIds
    }

    override fun removeVideofiles(vararg videofiles: Videofile) {
        videofileDao.delete(*videofiles)
    }

    override fun selectExistingFromUris(vararg uris: Uri): List<Uri> {
        return videofileDao.selectExistingFromUris(*uris)
    }
}