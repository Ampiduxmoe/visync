package com.example.visync.ui

import android.util.Log
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
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
import com.example.visync.connections.RunningConnection
import com.example.visync.connections.VisyncNearbyConnections
import com.example.visync.messaging.JsonVisyncMessageConverter
import com.example.visync.messaging.PlaybackPauseUnpauseMessage
import com.example.visync.messaging.PlaybackSeekToMessage
import com.example.visync.messaging.PlaybackSeekToPrevNextMessage
import com.example.visync.messaging.VisyncMessage
import com.example.visync.ui.components.navigation.VisyncNavigationActions
import com.example.visync.ui.components.navigation.TopLevelRoute
import com.example.visync.ui.screens.main.MainApp
import com.example.visync.ui.screens.main.MainAppViewModel
import com.example.visync.ui.screens.main.playback_setup.PlaybackSetupViewModel
import com.example.visync.ui.screens.player.VideoConfiguration
import com.example.visync.ui.screens.player.VisyncPlayer
import com.example.visync.ui.screens.player.VisyncPlayerViewModel

@Composable
fun AppWrapper(
    windowSize: WindowSizeClass,
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
    val visyncPLayerPlaybackState by visyncPlayerViewModel
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
            VisyncPlayer(
                playerUiState = visyncPlayerUiState,
                playerPlaybackState = visyncPLayerPlaybackState,
                playerPlaybackControls = visyncPlayerViewModel.playerWrapper.playbackControls,
                videoConfiguration = finalPlaybackSetupOutput.value!!.videoConfiguration,
                isUserHost = finalPlaybackSetupOutput.value!!.isUserHost,
                physicalDevice = sideNavigationUiState.editablePhysicalDevice.value,
                showOverlay = visyncPlayerViewModel::showOverlay,
                hideOverlay = visyncPlayerViewModel::hideOverlay,
                closePlayer = {
                    topLevelNavigationActions.navigateTo(TopLevelRoute.MainApp.routeString)
                    finalPlaybackSetupOutput.value!!.resetAllConnections()
                },
                player = visyncPlayerViewModel.playerWrapper.getPlayer(),
                messageSender = finalPlaybackSetupOutput.value!!.messageSender
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

fun getPlayerMessageSender(
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

class PlaybackSetupOutput(
    val videoConfiguration: VideoConfiguration,
    val isUserHost: Boolean,
    val resetAllConnections: () -> Unit,
    val messageSender: PlayerMessageSender,
)