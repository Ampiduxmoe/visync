package com.example.visync.ui.components.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.visync.data.playlists.Playlist
import com.example.visync.data.playlists.PlaylistWithVideofiles

@Composable
fun PlaylistItem(
    playlistWithVideofiles: PlaylistWithVideofiles,
    openPlaylist: (Playlist) -> Unit,
) {
    val playlist = playlistWithVideofiles.playlist
    val videofiles = playlistWithVideofiles.videofiles
    Column(
        modifier = Modifier.clickable { openPlaylist(playlist) }
    ) {
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = "${videofiles.count()} files",
            style = MaterialTheme.typography.labelMedium
        )
    }
}