package com.example.visync.data.playlists

import com.example.visync.data.videofiles.Videofile

data class PlaylistWithVideofiles(
    val playlist: Playlist,
    val videofiles: List<Videofile>
)
