package com.example.visync.ui

import android.util.Log
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
import com.example.visync.connections.RunningConnection
import com.example.visync.connections.VisyncNearbyConnections
import com.example.visync.messaging.JsonVisyncMessageConverter
import com.example.visync.messaging.PlaybackPauseUnpauseMessage
import com.example.visync.messaging.PlaybackSeekToMessage
import com.example.visync.messaging.PlaybackSeekToPrevNextMessage
import com.example.visync.messaging.VisyncMessage
import com.example.visync.ui.components.navigation.AppNavigationActions
import com.example.visync.ui.components.navigation.TopLevelRoute
import com.example.visync.ui.screens.main.MainApp
import com.example.visync.ui.screens.main.MainAppViewModel
import com.example.visync.ui.screens.main.RoomDiscoveringActions
import com.example.visync.ui.screens.main.VisyncPlaybackMode
import com.example.visync.ui.screens.main.playback_setup.PlaybackSetupViewModel
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
                },
                roomDiscoveringActions = RoomDiscoveringActions(
                    startDiscoveringClean = {
                        val username = sideNavigationUiState.editableUsername.value
                        playbackSetupViewModel.fullResetToGuestMode()
                        playbackSetupConnections.startDiscovering(username, context)
                    },
                    stopDiscovering = {
                        playbackSetupConnections.stopDiscovering()
                    },
                    joinRoom = {
                    }
                )
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

interface PlayerMessageSender {
    fun sendSeekToPrevMessage()
    fun sendSeekToNextMessage()
    fun sendPauseMessage()
    fun sendUnpauseMessage()
    fun sendSeekToMessage(seekTo: Float)
    fun sendSeekToMessage(seekTo: Long)
}

fun getHostPlayerMessageSender(
    playbackSetupViewModel: PlaybackSetupViewModel,
    playbackSetupConnections: VisyncNearbyConnections
) = object : PlayerMessageSender {

    private val messageConverter = JsonVisyncMessageConverter()

    private fun getApprovedWatchersConnections(): List<RunningConnection> {
        val approvedIds = playbackSetupViewModel.playbackSetupState.value.otherWatchers
            .filter { it.isApproved }
            .map { it.endpointId }
        return playbackSetupConnections.connectionsState.value.runningConnections
            .filter { it.endpointId in approvedIds }
    }

    private inline fun <reified T: VisyncMessage> encodeAndSend(fullMessage: T) {
        Log.d("shit", "sending msg from player controls!!!!")
        val encodedMsg = messageConverter.encode(fullMessage)
        playbackSetupConnections.sendMessageToMultiple(
            msg = encodedMsg,
            receivers = getApprovedWatchersConnections()
        )
    }

    override fun sendSeekToPrevMessage() {
        val seekToMessage = PlaybackSeekToPrevNextMessage(toPrev = true)
        encodeAndSend(seekToMessage)
    }
    override fun sendSeekToNextMessage() {
        val seekToMessage = PlaybackSeekToPrevNextMessage(toPrev = false)
        encodeAndSend(seekToMessage)
    }
    override fun sendPauseMessage() {
        val pauseUnpauseMessage = PlaybackPauseUnpauseMessage(doPause = true)
        encodeAndSend(pauseUnpauseMessage)
    }
    override fun sendUnpauseMessage() {
        val pauseUnpauseMessage = PlaybackPauseUnpauseMessage(doPause = false)
        encodeAndSend(pauseUnpauseMessage)
    }
    override fun sendSeekToMessage(seekTo: Float) {
        val seekToMessage = PlaybackSeekToMessage(
            seekToLong = null,
            seekToFloat = seekTo
        )
        encodeAndSend(seekToMessage)
    }
    override fun sendSeekToMessage(seekTo: Long) {
        val seekToMessage = PlaybackSeekToMessage(
            seekToLong = seekTo,
            seekToFloat = null
        )
        encodeAndSend(seekToMessage)
    }
}