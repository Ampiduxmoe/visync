package com.example.visync.ui.screens.main

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.visync.data.playlists.PlaylistWithVideofiles
import com.example.visync.ui.components.navigation.Route
import com.example.visync.ui.components.navigation.MainAppNavigation
import com.example.visync.ui.components.navigation.NavigationType
import com.example.visync.ui.screens.main.playlists.PlaylistsScreen
import com.example.visync.ui.screens.main.playlists.PlaylistsScreenViewModel
import com.example.visync.ui.screens.main.rooms.RoomsScreen
import com.example.visync.ui.screens.main.rooms.RoomsScreenViewModel

@Composable
fun MainApp(
    windowSize: WindowSizeClass,
    mainAppUiState: MainAppUiState,
    playPlaylist: (playlist: PlaylistWithVideofiles, startFrom: Int) -> Unit,
) {
    val navigationType: NavigationType
    val preferredDisplayMode: ContentDisplayMode

    Log.i("WindowSize", "widthSizeClass=${windowSize.widthSizeClass}")
    Log.i("WindowSize", "heightSizeClass=${windowSize.heightSizeClass}")

    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            navigationType = NavigationType.BOTTOM_NAVBAR_AND_DRAWER
            preferredDisplayMode = ContentDisplayMode.SINGLE_COLUMN
        }
        WindowWidthSizeClass.Medium -> {
            navigationType = NavigationType.RAIL_AND_DRAWER
            preferredDisplayMode = ContentDisplayMode.SINGLE_COLUMN
        }
        WindowWidthSizeClass.Expanded -> {
            navigationType = NavigationType.CUSTOM_PERMANENT_DRAWER
            preferredDisplayMode = ContentDisplayMode.DUAL_COLUMN
        }
        else -> {
            navigationType = NavigationType.BOTTOM_NAVBAR_AND_DRAWER
            preferredDisplayMode = ContentDisplayMode.SINGLE_COLUMN
        }
    }

    val navController = rememberNavController()
    MainAppNavigation(
        navigationType = navigationType,
        navController = navController
    ) {
        NavHost(
            navController = navController,
            startDestination = Route.Playlists.routeString,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Route.Playlists.routeString) {
                val playlistsScreenViewModel = hiltViewModel<PlaylistsScreenViewModel>()
                val playlistsUiState by playlistsScreenViewModel
                    .uiState.collectAsStateWithLifecycle()
                PlaylistsScreen(
                    preferredDisplayMode = preferredDisplayMode,
                    playlistsUiState = playlistsUiState,
                    openPlaylist = { playlistsScreenViewModel.setSelectedPlaylist(it.id) },
                    closePlaylist = playlistsScreenViewModel::closeDetailScreen,
                    addVideoToPlaylistFromUri = playlistsScreenViewModel::addVideoToPlaylistFromUri,
                    playVideofile = { videofile ->
                        val parentPlaylistWithVideofiles = playlistsUiState.playlists
                            .find { playlistWithVideofiles ->
                                playlistWithVideofiles.playlist.id == videofile.playlistId
                            }!! // since videofile always belongs to a playlist
                        val videofileIndex = parentPlaylistWithVideofiles.videofiles
                            .indexOf(videofile)
                        playPlaylist(
                            /* playlist = */ parentPlaylistWithVideofiles,
                            /* startFrom = */ videofileIndex
                        )
                    },
                    openDrawer = openDrawer
                )
            }
            composable(Route.RoomsJoin.routeString) {
                val roomsScreenViewModel = hiltViewModel<RoomsScreenViewModel>()
                val roomsUiState by roomsScreenViewModel
                    .uiState.collectAsStateWithLifecycle()
                RoomsScreen(
                    roomsUiState = roomsUiState
                )
            }
            composable(Route.MyProfile.routeString) {
                Column {

                }
            }
            composable(Route.Friends.routeString) {
                Column {

                }
            }
            composable(Route.RoomsManage.routeString) {
                Column {

                }
            }
            composable(Route.AppSettings.routeString) {
                Column {

                }
            }
        }
    }
}

enum class ContentDisplayMode {
    SINGLE_COLUMN, DUAL_COLUMN
}
