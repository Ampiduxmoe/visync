package com.example.visync.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.example.visync.ui.components.lists.PlaylistItem

@Composable
fun PlaylistsScreen(
    playlistsScreenViewModel: PlaylistsScreenViewModel
) {
    Column {
        val playlists = playlistsScreenViewModel.playlists
        for (playlist in playlists) {
            PlaylistItem(playlist)
        }
    }
}