package com.example.visync.ui

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.visync.ui.components.navigation.VisyncNavigationActions
import com.example.visync.ui.components.navigation.TopLevelRoute
import com.example.visync.ui.screens.main.MainApp
import com.example.visync.ui.screens.main.MainAppViewModel
import com.example.visync.ui.screens.player.VideoConfiguration
import com.example.visync.ui.screens.player.VisyncPlayer
import com.example.visync.ui.screens.player.VisyncPlayerViewModel

@Composable
fun AppWrapper(
    windowSize: WindowSizeClass,
    isDarkTheme: Boolean,
    setDarkTheme: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    val topLevelNavController = rememberNavController()
    val topLevelNavigationActions = remember(topLevelNavController) {
        VisyncNavigationActions(topLevelNavController)
    }

    val mainAppViewModel = hiltViewModel<MainAppViewModel>()
    val mainAppUiState by mainAppViewModel
        .uiState.collectAsStateWithLifecycle()
    val sideNavigationUiState by mainAppViewModel
        .mainAppNavigationUiState.collectAsStateWithLifecycle()
    val navStateUsername = sideNavigationUiState.editableUsername.value
    val navStatePhysicalDevice = sideNavigationUiState.editablePhysicalDevice.value
    val navStateHasPlaceholders =
            navStateUsername == mainAppViewModel.usernamePlaceholder ||
            navStatePhysicalDevice == mainAppViewModel.physicalDevicePlaceholder
    if (navStateHasPlaceholders) {
        mainAppViewModel.initializeNavigationUiState(context)
    }

    val visyncPlayerViewModel = hiltViewModel<VisyncPlayerViewModel>()
    val visyncPlayerUiState by visyncPlayerViewModel
        .uiState.collectAsStateWithLifecycle()
    val visyncPlayerPlaybackState by visyncPlayerViewModel
        .playerWrapper.playbackState.collectAsStateWithLifecycle()

    val finalPlaybackSetupOutput = remember { mutableStateOf<PlaybackSetupOutput?>(null) }

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
            val playbackControls = visyncPlayerViewModel.playerWrapper.playbackControls
            MainApp(
                windowSize = windowSize,
                isDarkTheme = isDarkTheme,
                setDarkTheme = setDarkTheme,
                mainAppUiState = mainAppUiState,
                mainAppNavigationUiState = sideNavigationUiState,
                play = { playbackStartOptions, playbackSetupOutput ->
                    finalPlaybackSetupOutput.value = playbackSetupOutput
                    playbackStartOptions.let {
                        playbackControls.setPlaybackSpeed(it.playbackSpeed)
                        visyncPlayerViewModel.setVideofilesToPlay(it.videofiles, it.startFrom)
                    }
                    topLevelNavigationActions.navigateTo(TopLevelRoute.Player.routeString)
                },
                playbackControls = playbackControls
            )
        }
        composable(
            route = TopLevelRoute.Player.routeString,
            enterTransition = { fadeIn(snap(transitionDelayMillis)) },
            exitTransition = { ExitTransition.None }
        ) {
            val enterAsHost = finalPlaybackSetupOutput.value!!.isUserHost
            DisposableEffect(Unit) {
                if (enterAsHost) {
                    visyncPlayerViewModel.listenToNearbyConnectionsAsHost()
                    visyncPlayerViewModel.startPinging()
                } else {
                    visyncPlayerViewModel.listenToNearbyConnectionsAsGuest()
                }
                onDispose {
                    visyncPlayerViewModel.stopPinging()
                    visyncPlayerViewModel.stopListeningToNearbyConnections()
                }
            }
            VisyncPlayer(
                playerUiState = visyncPlayerUiState,
                playerPlaybackState = visyncPlayerPlaybackState,
                playerPlaybackControls = visyncPlayerViewModel.playerWrapper.playbackControls,
                videoConfiguration = finalPlaybackSetupOutput.value!!.videoConfiguration,
                isUserHost = enterAsHost,
                hostMessenger = visyncPlayerViewModel.hostPlayerMessenger,
                physicalDevice = sideNavigationUiState.editablePhysicalDevice.value,
                showOverlay = visyncPlayerViewModel::showOverlay,
                hideOverlay = visyncPlayerViewModel::hideOverlay,
                closePlayer = {
                    topLevelNavigationActions.navigateTo(TopLevelRoute.MainApp.routeString)
                    finalPlaybackSetupOutput.value!!.resetAllConnections()
                },
                player = visyncPlayerViewModel.playerWrapper.getPlayer(),
            )
        }
    }
}

class PlaybackSetupOutput(
    val videoConfiguration: VideoConfiguration,
    val isUserHost: Boolean,
    val resetAllConnections: () -> Unit,
)