package com.example.visync.ui.components.lists

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.visync.data.playlists.Playlist

@Composable
fun PlaylistItem(
    playlist: Playlist
) {
    Row {
        Text(text = playlist.name)
    }
}