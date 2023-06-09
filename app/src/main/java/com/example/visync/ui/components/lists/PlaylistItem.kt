package com.example.visync.ui.components.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.visync.data.playlists.Playlist
import com.example.visync.data.playlists.PlaylistWithVideofiles

@Composable
fun PlaylistItem(
    playlistWithVideofiles: PlaylistWithVideofiles,
    onClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playlist = playlistWithVideofiles.playlist
    val videofiles = playlistWithVideofiles.videofiles
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onClick(playlist) }
    ) {
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "${videofiles.count()} files",
            style = MaterialTheme.typography.titleSmall
        )
    }
}