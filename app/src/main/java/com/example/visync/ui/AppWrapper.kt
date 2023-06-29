package com.example.visync.ui

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.visync.ui.components.navigation.AppNavigationActions
import com.example.visync.ui.components.navigation.TopLevelRoute
import com.example.visync.ui.screens.main.MainApp
import com.example.visync.ui.screens.main.MainAppViewModel
import com.example.visync.ui.screens.main.VisyncPlaybackMode
import com.example.visync.ui.screens.player.PlaybackSetupScreen
import com.example.visync.ui.screens.player.PlaybackSetupViewModel
import com.example.visync.ui.screens.player.VisyncPlayer
import com.example.visync.ui.screens.player.VisyncPlayerViewModel

@Composable
fun AppWrapper(
    windowSize: WindowSizeClass,
) {
    val topLevelNavController = rememberNavController()
    val topLevelNavigationActions = remember(topLevelNavController) {
        AppNavigationActions(topLevelNavController)
    }

    val mainAppViewModel = hiltViewModel<MainAppViewModel>()
    val mainAppUiState by mainAppViewModel
        .uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    mainAppViewModel.initializeNavigationUiState(context)
    val sideNavigationUiState by mainAppViewModel
        .mainAppNavigationUiState.collectAsStateWithLifecycle()

    val playbackSetupViewModel = hiltViewModel<PlaybackSetupViewModel>()
    val playbackSetupState by playbackSetupViewModel
        .playbackSetupState.collectAsStateWithLifecycle()
    val playbackSetupConnections = playbackSetupViewModel.visyncNearbyConnections
    val playbackSetupConnectionsState by playbackSetupConnections
        .connectionsState.collectAsStateWithLifecycle()

    val visyncPlayerViewModel = hiltViewModel<VisyncPlayerViewModel>()
    val visyncPlayerUiState by visyncPlayerViewModel
        .uiState.collectAsStateWithLifecycle()
    val visyncPLayerPlaybackState by visyncPlayerViewModel
        .playerWrapper.playbackState.collectAsStateWithLifecycle()

    /**
     *  How much time to give to composition process
     *  of top level destination's composable to get its job done.
     *  If there is no delay we see two destination screens overlapping
     *  (for one or two frames) and UI elements like bottom navbar jumping
     *  to adjust their position.
     */
    val transitionDelayMillis = 25

    NavHost(
        navController = topLevelNavController,
        startDestination = TopLevelRoute.MainApp.routeString,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(
            route = TopLevelRoute.MainApp.routeString,
            enterTransition = { fadeIn(snap(transitionDelayMillis)) },
            exitTransition = { ExitTransition.None }
        ) {
            MainApp(
                windowSize = windowSize,
                mainAppUiState = mainAppUiState,
                mainAppNavigationUiState = sideNavigationUiState,
                playPlaylist = { options ->
                    visyncPlayerViewModel.setVideofilesToPlay(
                        videofilesToPlay = options.playlist.videofiles,
                        startFrom = options.startFrom
                    )
                    when (options.playbackMode) {
                        VisyncPlaybackMode.ALONE -> {
                            topLevelNavigationActions.navigateTo(TopLevelRoute.Player.routeString)
                        }
                        VisyncPlaybackMode.GROUP -> {
                            val connectionsAdvertiser = playbackSetupViewModel.connectionsAdvertiser
                            val advertiserState = connectionsAdvertiser.advertiserState.value
                            if (!advertiserState.isAdvertising) {
                                val username = sideNavigationUiState.editableUsername.value
                                connectionsAdvertiser.startAdvertising(username, context)
                            }
                            playbackSetupViewModel.allowSetupEditing()
                            topLevelNavigationActions.navigateTo(TopLevelRoute.PlaybackSetup.routeString)
                        }
                    }
                }
            )
        }
        composable(
            route = TopLevelRoute.PlaybackSetup.routeString,
            enterTransition = { fadeIn(snap(transitionDelayMillis)) },
            exitTransition = { ExitTransition.None }
        ) {
            PlaybackSetupScreen(
                playbackSetupState = playbackSetupState,
                connectedUsers = playbackSetupConnectionsState.runningConnections,
                openPlayer = {
                    val isUserHosting = playbackSetupState.canChangePlaybackSettings
                    if (isUserHosting) {
                        playbackSetupViewModel.sendOpenPlayer()
                        topLevelNavigationActions.navigateTo(TopLevelRoute.Player.routeString)
                        playbackSetupViewModel.connectionsAdvertiser.stopAdvertising()
                    }
                }
            )
        }
        composable(
            route = TopLevelRoute.Player.routeString,
            enterTransition = { fadeIn(snap(transitionDelayMillis)) },
            exitTransition = { ExitTransition.None }
        ) {
            VisyncPlayer(
                playerUiState = visyncPlayerUiState,
                playerPlaybackState = visyncPLayerPlaybackState,
                playerPlaybackControls = visyncPlayerViewModel.playerWrapper.playbackControls,
                showOverlay = visyncPlayerViewModel::showOverlay,
                hideOverlay = visyncPlayerViewModel::hideOverlay,
                closePlayer = {
                    topLevelNavigationActions.navigateTo(TopLevelRoute.MainApp.routeString)
                    playbackSetupViewModel.connectionsAdvertiser.stop()
                },
                player = visyncPlayerViewModel.playerWrapper.getPlayer()
            )
        }
    }
}