package com.example.visync.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.visync.data.playlists.Playlist
import com.example.visync.data.playlists.PlaylistWithVideofiles
import com.example.visync.ui.components.lists.PlaylistItem

@Composable
fun PlaylistsScreen(
    playlistsUiState: PlaylistsUiState,
    openPlaylist: (Playlist) -> Unit,
    closePlaylist: () -> Unit,
) {
    playlistsUiState.selectedPlaylist?.let {
        BackHandler {
            closePlaylist()
        }
        PlaylistDetailsScreen(it)
        return
    }
    Column {
        val playlists = playlistsUiState.playlists
        for (playlist in playlists) {
            PlaylistItem(
                playlistWithVideofiles = playlist,
                openPlaylist = openPlaylist
            )
        }
    }
}

@Composable
fun PlaylistDetailsScreen(
    playlistWithVideofiles: PlaylistWithVideofiles
) {
    Column {
        Text(
            text = playlistWithVideofiles.playlist.name,
            style = MaterialTheme.typography.titleLarge
        )
        Column {
            for (videofile in playlistWithVideofiles.videofiles) {
                Text(
                    text = "file: ${videofile.filename}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}