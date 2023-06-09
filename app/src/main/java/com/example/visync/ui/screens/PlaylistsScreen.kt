package com.example.visync.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.visync.R
import com.example.visync.data.playlists.Playlist
import com.example.visync.data.playlists.PlaylistWithVideofiles
import com.example.visync.data.videofiles.Videofile
import com.example.visync.ui.ContentDisplayMode
import com.example.visync.ui.components.VisyncTopAppBar
import com.example.visync.ui.components.lists.PlaylistItem
import com.example.visync.ui.components.lists.VideofileItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    preferredDisplayMode: ContentDisplayMode,
    playlistsUiState: PlaylistsUiState,
    openPlaylist: (Playlist) -> Unit,
    closePlaylist: () -> Unit,
    addVideoToPlaylistFromUri: (Playlist, Uri) -> Unit,
    playVideofile: (Videofile) -> Unit,
    openDrawer: () -> Unit,
) {
    when (preferredDisplayMode) {
        ContentDisplayMode.SINGLE_COLUMN -> {
            playlistsUiState.selectedPlaylist?.let { playlistWithVideofiles ->
                BackHandler {
                    closePlaylist()
                }
                Column {
                    VisyncTopAppBar(
                        title = playlistWithVideofiles.playlist.name,
                        navigationButtonAction = closePlaylist,
                        navigationButtonIcon = {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.desc_back)
                            )
                        }
                    )
                    PlaylistDetails(
                        playlistWithVideofiles = playlistWithVideofiles,
                        addVideoToPlaylistFromUri = addVideoToPlaylistFromUri,
                        playVideofile = playVideofile
                    )
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
                Playlists(
                    playlists = playlistsUiState.playlists,
                    selectedPlaylist = null,
                    playlistOnClick = openPlaylist
                )
            }
        }
        ContentDisplayMode.DUAL_COLUMN -> {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                Playlists(
                    playlists = playlistsUiState.playlists,
                    selectedPlaylist = playlistsUiState.selectedPlaylist,
                    playlistOnClick = {
                        if (playlistsUiState.selectedPlaylist?.playlist == it) {
                            closePlaylist()
                        } else {
                            openPlaylist(it)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                )
                AnimatedVisibility(
                    visible = playlistsUiState.selectedPlaylist != null,
                    enter = expandHorizontally(expandFrom = Alignment.Start) { 0 }
                            + fadeIn(initialAlpha = 0f),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.Start) { 0 }
                            + fadeOut(targetAlpha = 0f),
                ) {
                    val lastSelection = remember {
                        mutableStateOf(
                            PlaylistWithVideofiles(
                                playlist = Playlist(
                                    id = -1,
                                    name = ""
                                ),
                                videofiles = listOf()
                            )
                        )
                    }
                    LaunchedEffect(playlistsUiState.selectedPlaylist) {
                        playlistsUiState.selectedPlaylist?.let {
                            lastSelection.value = it
                        }
                    }
                    PlaylistDetails(
                        playlistWithVideofiles = lastSelection.value,
                        addVideoToPlaylistFromUri = addVideoToPlaylistFromUri,
                        playVideofile = playVideofile,
                        modifier = Modifier.fillMaxWidth(0.5f).padding(8.dp)
                    )
                }
            }
        }
    }

}

@Composable
fun Playlists(
    playlists: List<PlaylistWithVideofiles>,
    selectedPlaylist: PlaylistWithVideofiles?,
    playlistOnClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(playlists) { playlist ->
            PlaylistItem(
                playlistWithVideofiles = playlist,
                onClick = playlistOnClick,
                modifier = if (playlist != selectedPlaylist) Modifier else
                    Modifier.background(
                        color = Red,
                        shape = RectangleShape
                    )
            )
        }
    }
}

@Composable
fun PlaylistDetails(
    playlistWithVideofiles: PlaylistWithVideofiles,
    addVideoToPlaylistFromUri: (Playlist, Uri) -> Unit,
    playVideofile: (Videofile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                addVideoToPlaylistFromUri(
                    playlistWithVideofiles.playlist,
                    uri
                )
            }
        }
    )
    Column(
        modifier = modifier
    ) {
        IconButton(onClick = { selectVideoLauncher.launch("video/*") }) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(id = R.string.desc_add_video_to_playlist)
            )
        }
        LazyColumn {
            items(playlistWithVideofiles.videofiles) { videofile ->
                VideofileItem(
                    videofile = videofile,
                    onClick = { playVideofile(videofile) }
                )
            }
        }
    }
}