package com.example.visync.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.visync.R
import com.example.visync.data.playlists.Playlist
import com.example.visync.data.playlists.PlaylistWithVideofiles
import com.example.visync.ui.components.VisyncTopAppBar
import com.example.visync.ui.components.lists.PlaylistItem
import com.example.visync.ui.components.lists.VideofileItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    playlistsUiState: PlaylistsUiState,
    openPlaylist: (Playlist) -> Unit,
    closePlaylist: () -> Unit,
    openDrawer: () -> Unit,
) {
    playlistsUiState.selectedPlaylist?.let {
        BackHandler {
            closePlaylist()
        }
        Column {
            VisyncTopAppBar(
                title = it.playlist.name,
                navigationButtonAction = closePlaylist,
                navigationButtonIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.desc_back)
                    )
                }
            )
            PlaylistDetailsScreen(it)
        }

        return
    }

    Column {
        VisyncTopAppBar(
            title = stringResource(id = R.string.tab_name_playlists),
            navigationButtonAction = openDrawer,
            navigationButtonIcon = {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(id = R.string.desc_navigation_drawer)
                )
            }
        )
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

}

@Composable
fun PlaylistDetailsScreen(
    playlistWithVideofiles: PlaylistWithVideofiles
) {
    Column {
        for (videofile in playlistWithVideofiles.videofiles) {
            VideofileItem(
                videofile = videofile,
                onClick = {}
            )
        }
    }
}