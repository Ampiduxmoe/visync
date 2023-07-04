package com.example.visync.ui

import android.content.Context
import androidx.activity.compose.BackHandler
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
import com.example.visync.connections.DiscoveredEndpoint
import com.example.visync.connections.VisyncNearbyConnectionsState
import com.example.visync.connections.VisyncNearbyConnections
import com.example.visync.ui.components.navigation.AppNavigationActions
import com.example.visync.ui.components.navigation.TopLevelRoute
import com.example.visync.ui.screens.main.MainApp
import com.example.visync.ui.screens.main.MainAppViewModel
import com.example.visync.ui.screens.main.PlaybackStartOptions
import com.example.visync.ui.screens.main.RoomDiscoveringOptions
import com.example.visync.ui.screens.main.VisyncPlaybackMode
import com.example.visync.ui.screens.player.PlaybackSetupScreen
import com.example.visync.ui.screens.player.PlaybackSetupViewModel
import com.example.visync.ui.screens.player.SetupMode
import com.example.visync.ui.screens.player.VisyncPlayer
import com.example.visync.ui.screens.player.VisyncPlayerViewModel

@Composable
fun AppWrapper(
    windowSize: WindowSizeClass,
) {
    val context = LocalContext.current

    val topLevelNavController = rememberNavController()
    val topLevelNavigationActions = remember(topLevelNavController) {
        AppNavigationActions(topLevelNavController)
    }

    val mainAppViewModel = hiltViewModel<MainAppViewModel>()
    val mainAppUiState by mainAppViewModel
        .uiState.collectAsStateWithLifecycle()
    val sideNavigationUiState by mainAppViewModel
        .mainAppNavigationUiState.collectAsStateWithLifecycle()
    if (sideNavigationUiState.editableUsername.value == mainAppViewModel.usernamePlaceholder) {
        mainAppViewModel.initializeNavigationUiState(context)
    }

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
                    if (options.playbackMode == VisyncPlaybackMode.ALONE) {
                        topLevelNavigationActions.navigateTo(TopLevelRoute.Player.routeString)
                        return@MainApp
                    }
                    transitionToPlaybackSetupAsHost(
                        context = context,
                        username = sideNavigationUiState.editableUsername.value,
                        playbackStartOptions = options,
                        topLevelNavigationActions = topLevelNavigationActions,
                        visyncPlayerViewModel = visyncPlayerViewModel,
                        playbackSetupViewModel = playbackSetupViewModel,
                        playbackSetupConnections = playbackSetupConnections,
                        playbackSetupConnectionsState = playbackSetupConnectionsState
                    )
                },
                roomDiscoveringOptions = RoomDiscoveringOptions(
                    startDiscoveringClean = {
                        val username = sideNavigationUiState.editableUsername.value
                        playbackSetupViewModel.fullResetToGuestMode()
                        playbackSetupConnections.startDiscovering(username, context)
                    },
                    stopDiscovering = {
                        playbackSetupConnections.stopDiscovering()
                    },
                    joinRoom = {
                        transitionToPlaybackSetupAsGuest(
                            connectTo = it,
                            topLevelNavigationActions = topLevelNavigationActions,
                            playbackSetupViewModel = playbackSetupViewModel
                        )
                    }
                )
            )
        }
        composable(
            route = TopLevelRoute.PlaybackSetup.routeString,
            enterTransition = { fadeIn(snap(transitionDelayMillis)) },
            exitTransition = { ExitTransition.None }
        ) {
            BackHandler {
                playbackSetupConnections.reset()
                playbackSetupViewModel.resetToDefaultState()
                topLevelNavigationActions.back()
            }
            PlaybackSetupScreen(
                playbackSetupState = playbackSetupState,
                approveWatcher = playbackSetupViewModel::approveWatcher,
                disapproveWatcher = playbackSetupViewModel::disapproveWatcher,
                openPlayer = {
                    if (playbackSetupState.setupMode == SetupMode.HOST) {
                        playbackSetupViewModel.sendOpenPlayer()
                        topLevelNavigationActions.navigateTo(TopLevelRoute.Player.routeString)
                        playbackSetupConnections.stopAdvertising()
                    }
                    if (playbackSetupState.setupMode == SetupMode.GUEST) {
                        playbackSetupViewModel.resetMessageEvents()
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
                    playbackSetupConnections.reset()
                },
                player = visyncPlayerViewModel.playerWrapper.getPlayer()
            )
        }
    }
}

fun transitionToPlaybackSetupAsHost(
    context: Context,
    username: String,
    playbackStartOptions: PlaybackStartOptions,
    topLevelNavigationActions: AppNavigationActions,
    visyncPlayerViewModel: VisyncPlayerViewModel,
    playbackSetupViewModel: PlaybackSetupViewModel,
    playbackSetupConnections: VisyncNearbyConnections,
    playbackSetupConnectionsState: VisyncNearbyConnectionsState,
) {
    visyncPlayerViewModel.setVideofilesToPlay(
        videofilesToPlay = playbackStartOptions.videofiles,
        startFrom = playbackStartOptions.startFrom
    )
    playbackSetupViewModel.fullResetToHostMode()
    if (!playbackSetupConnectionsState.isAdvertising) {
        playbackSetupConnections.startAdvertising(username, context)
    }
    topLevelNavigationActions.navigateTo(
        TopLevelRoute.PlaybackSetup.routeString
    )
}

fun transitionToPlaybackSetupAsGuest(
    connectTo: DiscoveredEndpoint,
    topLevelNavigationActions: AppNavigationActions,
    playbackSetupViewModel: PlaybackSetupViewModel,
) {
    playbackSetupViewModel.messageEvents.apply {
        onOpenPlayerMessage = {
            topLevelNavigationActions.navigateTo(
                TopLevelRoute.Player.routeString
            )
        }
        onSetVideofilesMessage = {

        }
    }
    topLevelNavigationActions.navigateTo(TopLevelRoute.PlaybackSetup.routeString)
    connectTo.initiateConnection()
}