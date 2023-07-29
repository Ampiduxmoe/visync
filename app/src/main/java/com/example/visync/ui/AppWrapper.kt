package com.example.visync.ui

import android.content.Context
import android.util.Log
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
import com.example.visync.connections.RunningConnection
import com.example.visync.connections.VisyncNearbyConnectionsState
import com.example.visync.connections.VisyncNearbyConnections
import com.example.visync.data.videofiles.Videofile
import com.example.visync.messaging.JsonVisyncMessageConverter
import com.example.visync.messaging.PlaybackPauseUnpauseMessage
import com.example.visync.messaging.PlaybackSeekToMessage
import com.example.visync.messaging.PlaybackSeekToPrevNextMessage
import com.example.visync.messaging.VisyncMessage
import com.example.visync.player.HostPlayerWrapperPlaybackControls
import com.example.visync.player.PlayerMessageSender
import com.example.visync.ui.components.navigation.AppNavigationActions
import com.example.visync.ui.components.navigation.TopLevelRoute
import com.example.visync.ui.screens.main.MainApp
import com.example.visync.ui.screens.main.MainAppViewModel
import com.example.visync.ui.screens.main.PlaybackStartOptions
import com.example.visync.ui.screens.main.RoomDiscoveringOptions
import com.example.visync.ui.screens.main.VisyncPlaybackMode
import com.example.visync.ui.screens.player.PlaybackSetupOptionSetters
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
                            visyncPlayerViewModel = visyncPlayerViewModel,
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
                playbackSetupOptionSetters = PlaybackSetupOptionSetters(
                    setSelectedFileIndex = { index ->
                        val currentVideofileNames = playbackSetupState
                            .playbackSetupOptions.videofileNames
                        visyncPlayerViewModel.setVideofilesToPlay(
                            videofilesToPlay = visyncPlayerUiState.currentPlaylist.map { it.key },
                            startFrom = index
                        )
                        playbackSetupViewModel.setVideofilesToPlayAndNotifyIfNeeded(
                            videofileNames = currentVideofileNames,
                            startFrom = index
                        )
                    },
                    setDoStream = { doStream ->
                        playbackSetupViewModel.setDoStream(doStream)
                    },
                    setPlaybackSpeed = { playbackSpeed ->
                        val playbackControls = visyncPlayerViewModel.playerWrapper.playbackControls
                        playbackControls.setPlaybackSpeed(playbackSpeed)
                        playbackSetupViewModel.setPlaybackSpeed(playbackSpeed)
                    },
                    toggleRepeatMode = {
                        val playbackControls = visyncPlayerViewModel.playerWrapper.playbackControls
                        val newRepeatMode = playbackControls.toggleRepeatMode()
                        playbackSetupViewModel.setRepeatMode(newRepeatMode)
                    }
                ),
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
                    val playbackControls = visyncPlayerViewModel.playerWrapper.playbackControls
                    if (playbackControls is HostPlayerWrapperPlaybackControls) {
                        playbackControls.removePlayerMessageSender()
                    }
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
    playbackSetupViewModel.fullResetToHostMode()
    visyncPlayerViewModel.setVideofilesToPlay(
        videofilesToPlay = playbackStartOptions.videofiles,
        startFrom = playbackStartOptions.startFrom
    )
    playbackSetupViewModel.setVideofilesToPlayNoNotify(
        videofileNames = playbackStartOptions.videofiles.map { it.metadata.filename },
        startFrom = playbackStartOptions.startFrom
    )
    val playbackControls = visyncPlayerViewModel.playerWrapper.playbackControls
    Log.d("shit", "checking")
    if (playbackControls is HostPlayerWrapperPlaybackControls) {
        Log.d("shit", "setting message sender")
        playbackControls.setPlayerMessageSender(getHostPlayerMessageSender(
            playbackSetupViewModel = playbackSetupViewModel,
            playbackSetupConnections = playbackSetupConnections
        ))
    }
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
    visyncPlayerViewModel: VisyncPlayerViewModel,
    playbackSetupViewModel: PlaybackSetupViewModel,
) {
    playbackSetupViewModel.messageEvents.apply {
        onOpenPlayerMessage = {
            topLevelNavigationActions.navigateTo(
                TopLevelRoute.Player.routeString
            )
        }
        onSetVideofilesMessage = { videofileNames, startFrom ->
            val videofilesRepository = visyncPlayerViewModel.videofilesRepository
            val videofiles =  emptyList<Videofile>() // videofilesRepository.videofiles.value
            TODO()
            val namesToVideofiles = videofileNames.associateWith { filename ->
                videofiles.find { it.metadata.filename == filename }
            }
            val selectedVideofile = namesToVideofiles.values.toList()[startFrom]
            Log.d("shit", "videofiles = $videofiles")
            Log.d("shit", "names to vids = $namesToVideofiles")
            val correctedVideofilesList = namesToVideofiles.values.filterNotNull().toList()
            val correctedIndex = when (selectedVideofile) {
                null -> 0
                else -> correctedVideofilesList.indexOf(selectedVideofile)
            }
            visyncPlayerViewModel.setVideofilesToPlay(correctedVideofilesList, correctedIndex)
            val missingVideofileNames = namesToVideofiles.filter { it.value == null }.keys.toList()
            missingVideofileNames
        }
        onPlaybackSetupOptionsUpdateMessage = { playbackSetupOptions ->
            val playbackControls = visyncPlayerViewModel.playerWrapper.playbackControls
            playbackControls.setPlaybackSpeed(playbackSetupOptions.playbackSpeed)
            playbackControls.setRepeatMode(playbackSetupOptions.repeatMode)
        }
        onPauseUnpauseMessage = { message ->
            val playbackControls = visyncPlayerViewModel.playerWrapper.playbackControls
            when (message.doPause) {
                true -> { playbackControls.pause() }
                false -> { playbackControls.unpause() }
            }
        }
    }
    topLevelNavigationActions.navigateTo(TopLevelRoute.PlaybackSetup.routeString)
    connectTo.initiateConnection()
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