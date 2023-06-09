package com.example.visync.ui

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.visync.ui.components.navigation.AppNavigationActions
import com.example.visync.ui.components.navigation.TopLevelRoute
import com.example.visync.ui.screens.PlayerScreenViewModel
import com.example.visync.ui.screens.VisyncPlayer

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

    val visyncPlayerViewModel = hiltViewModel<PlayerScreenViewModel>()
    val visyncPlayerUiState by visyncPlayerViewModel
        .uiState.collectAsStateWithLifecycle()

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
                playPlaylist = { playlistWithVideofiles, startFrom ->
                    visyncPlayerViewModel.setVideofilesToPlay(
                        videofilesToPlay = playlistWithVideofiles.videofiles,
                        startFrom = startFrom
                    )
                    topLevelNavigationActions.navigateTo(TopLevelRoute.Player.routeString)
                }
            )
        }
        composable(
            route = TopLevelRoute.Player.routeString,
            enterTransition = { fadeIn(snap(transitionDelayMillis)) },
            exitTransition = { ExitTransition.None }
        ) {
            VisyncPlayer(
                visyncPlayerUiState = visyncPlayerUiState,
                closePlayer = {
                    topLevelNavigationActions.navigateTo(TopLevelRoute.MainApp.routeString)
                }
            ) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).also {
                            it.player = visyncPlayerViewModel.getPlayer()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 9f)
                )
            }
        }
    }
}