package com.example.visync.ui.screens.main.playlists

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.Companion.isPhotoPickerAvailable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.visync.R
import com.example.visync.data.playlists.Playlist
import com.example.visync.data.playlists.PlaylistWithVideofiles
import com.example.visync.data.videofiles.Videofile
import com.example.visync.ui.components.VisyncTopAppBar
import com.example.visync.ui.components.lists.PlaylistItem
import com.example.visync.ui.components.lists.VideofileItem
import com.example.visync.ui.screens.main.ContentDisplayMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    preferredDisplayMode: ContentDisplayMode,
    playlistsUiState: PlaylistsUiState,
    openPlaylist: (Playlist) -> Unit,
    closePlaylist: () -> Unit,
    addPlaylist: (Playlist) -> Unit,
    addVideosToPlaylistFromUri: (Playlist, List<Uri>) -> Unit,
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
                        addVideosToPlaylistFromUri = addVideosToPlaylistFromUri,
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
                    playlistOnClick = openPlaylist,
                    addPlaylist = addPlaylist
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
                    addPlaylist = addPlaylist,
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
                                    playlistId = -1,
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
                        addVideosToPlaylistFromUri = addVideosToPlaylistFromUri,
                        playVideofile = playVideofile,
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .padding(8.dp)
                    )
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Playlists(
    playlists: List<Playlist>,
    selectedPlaylist: PlaylistWithVideofiles?,
    playlistOnClick: (Playlist) -> Unit,
    addPlaylist: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showCreatePlaylistDialog = remember { mutableStateOf(false) }
    if (showCreatePlaylistDialog.value) {
        AlertDialog(
            onDismissRequest = {
                // Dismiss the dialog when the user clicks outside the dialog or on the back
                // button. If you want to disable that functionality, simply use an empty
                // onDismissRequest.

            }
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "add playlist with this name.....",
                    )
                    val playlistNameInputText = remember { mutableStateOf("") }
                    TextField(
                        value = playlistNameInputText.value,
                        onValueChange = {
                            playlistNameInputText.value = it
                        }
                    )
                    Row {
                        TextButton(
                            onClick = {
                                showCreatePlaylistDialog.value = false
                                playlistNameInputText.value = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                if (playlistNameInputText.value != "") {
                                    addPlaylist(
                                        Playlist(
                                            playlistId = 0,
                                            name = playlistNameInputText.value,
                                        )
                                    )
                                }
                                showCreatePlaylistDialog.value = false
                                playlistNameInputText.value = ""
                            }
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
    Column {
        IconButton(onClick = { showCreatePlaylistDialog.value = true }) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(id = R.string.desc_add_playlist)
            )
        }
        LazyColumn(
            modifier = modifier
        ) {
            items(playlists) { playlist ->
                PlaylistItem(
                    playlist = playlist,
                    onClick = playlistOnClick,
                    modifier = if (playlist != selectedPlaylist?.playlist) Modifier
                    else Modifier.background(
                        color = Red,
                        shape = RectangleShape
                    )
                )
            }
        }
    }
}

@Composable
fun PlaylistDetails(
    playlistWithVideofiles: PlaylistWithVideofiles,
    addVideosToPlaylistFromUri: (Playlist, List<Uri>) -> Unit,
    playVideofile: (Videofile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modernVideoPickerLauncher =  rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            uris.ifEmpty { return@rememberLauncherForActivityResult }
            addVideosToPlaylistFromUri(playlistWithVideofiles.playlist, uris)
        }
    )
    val selectFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let {
                TODO()
            }
        }
    )
    Column(
        modifier = modifier
    ) {
        IconButton(
            onClick = {
                val videoOnly = ActivityResultContracts.PickVisualMedia.VideoOnly
                modernVideoPickerLauncher.launch(PickVisualMediaRequest(videoOnly))
            }
        ) {
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