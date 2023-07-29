package com.example.visync.ui.screens.main.playback_setup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.media3.common.Player
import com.example.visync.connections.ConnectionRequest
import com.example.visync.connections.RunningConnection
import com.example.visync.connections.VisyncNearbyConnections
import com.example.visync.connections.VisyncNearbyConnectionsListener
import com.example.visync.messaging.JsonVisyncMessageConverter
import com.example.visync.messaging.OpenPlayerMessage
import com.example.visync.messaging.RequestVersionMessage
import com.example.visync.messaging.AllWatchersUpdateMessage
import com.example.visync.messaging.DoNotHaveVideofilesMessage
import com.example.visync.messaging.PlaybackPauseUnpauseMessage
import com.example.visync.messaging.PlaybackSetupOptionsUpdateMessage
import com.example.visync.messaging.RequestSelfInfoMessage
import com.example.visync.messaging.SelfInfoMessage
import com.example.visync.messaging.VersionMessage
import com.example.visync.messaging.VisyncMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import javax.inject.Inject

@HiltViewModel
class PlaybackSetupViewModel @Inject constructor(
    val visyncNearbyConnections: VisyncNearbyConnections,
) : ViewModel() {

    val connectionsAdvertiser = visyncNearbyConnections.asAdvertiser()
    val connectionsDiscoverer = visyncNearbyConnections.asDiscoverer()

    val _allRunningConnections
        get() = visyncNearbyConnections.connectionsState.value.runningConnections

    private val messagingVersion = 1
    private val setupModeDefault = SetupMode.HOST
    private val meAsWatcherDefault = Watcher(
        endpointId = "",
        username = "",
        messagingVersion = messagingVersion,
        isApproved = false,
        missingVideofileNames = listOf()
    )
    private val playbackSetupStateDefault = PlaybackSetupState(
        setupMode = setupModeDefault,
        isConnectingToHost = false,
        connectionError = false,
        hostAsWatcher = meAsWatcherDefault,
        meAsWatcher = meAsWatcherDefault,
        otherWatchers = listOf(),
        playbackSetupOptions = PlaybackSetupOptions(
            videofileNames = listOf(),
            selectedVideofileIndex = -1,
            doStream = false,
            playbackSpeed = 1f,
            repeatMode = Player.REPEAT_MODE_OFF,
        ),
    )

    private val _playbackSetupState = MutableStateFlow(playbackSetupStateDefault)
    val playbackSetupState: StateFlow<PlaybackSetupState> = _playbackSetupState

    private var _setupMode
        get() = _playbackSetupState.value.setupMode
        set(value) = _playbackSetupState.update { it.copy(setupMode = value) }
    private var _isConnectingToHost
        get() = _playbackSetupState.value.isConnectingToHost
        set(value) = _playbackSetupState.update { it.copy(isConnectingToHost = value) }
    private var _connectionError
        get() = _playbackSetupState.value.connectionError
        set(value) = _playbackSetupState.update { it.copy(connectionError = value) }
    private var _hostAsWatcher
        get() = _playbackSetupState.value.hostAsWatcher
        set(value) = _playbackSetupState.update { it.copy(hostAsWatcher = value) }
    private var _meAsWatcher
        get() = _playbackSetupState.value.meAsWatcher
        set(value) = _playbackSetupState.update { it.copy(meAsWatcher = value) }
    private var _otherWatchers
        get() = _playbackSetupState.value.otherWatchers
        set(value) = _playbackSetupState.update { it.copy(otherWatchers = value) }
    private var _playbackSetupOptions
        get() = _playbackSetupState.value.playbackSetupOptions
        set(value) = _playbackSetupState.update { it.copy(playbackSetupOptions = value) }

    var messageEvents = getEmptyMessageEvents()
        private set

    init {
        visyncNearbyConnections.setEventListener(object : VisyncNearbyConnectionsListener() {
            override fun onNewConnectionRequest(request: ConnectionRequest) {
                request.accept()
            }

            override fun onConnectionError(endpoint: String) {
                if (_setupMode == SetupMode.GUEST) {
                    _connectionError = true
                }
            }
            override fun onNewRunningConnection(connection: RunningConnection) {
                if (_meAsWatcher.endpointId == meAsWatcherDefault.endpointId) {
                    playbackSetupMessenger.sendRequestSelfInfo(connection)
                }
                if (_setupMode == SetupMode.HOST) {
                    playbackSetupMessenger.sendRequestMessagingVersion(connection)
                }
                if( _setupMode == SetupMode.GUEST && _isConnectingToHost) {
                    _isConnectingToHost = false
                }
            }
            override fun onRunningConnectionLost(connection: RunningConnection) {
                removeWatcher(connection.endpointId)
                if( _setupMode == SetupMode.GUEST && !_isConnectingToHost) {
                    _isConnectingToHost = true
                }
            }
            override fun onNewMessage(message: String, from: RunningConnection) {
                playbackSetupMessenger.process(message, from)
            }
        })
        observeAdvertiserState()
    }

    private fun observeAdvertiserState() {
//        viewModelScope.launch {
//            connectionsAdvertiser.advertiserState.collect { advertiserState ->
//
//            }
//        }
    }

    fun resetToDefaultState() {
        _playbackSetupState.update { playbackSetupStateDefault }
    }

    fun fullResetToHostMode() {
        visyncNearbyConnections.reset()
        _playbackSetupState.value = playbackSetupStateDefault.copy(
            setupMode = SetupMode.HOST
        )
    }

    fun fullResetToGuestMode() {
        visyncNearbyConnections.reset()
        _playbackSetupState.value = playbackSetupStateDefault.copy(
            setupMode = SetupMode.GUEST,
            isConnectingToHost = true,
            connectionError = false
        )
    }

    private fun addWatcher(watcher: Watcher) {
        _otherWatchers = _otherWatchers + watcher
        if (_setupMode == SetupMode.HOST) {
            playbackSetupMessenger.sendAllWatchersUpdate()
        }
    }

    private fun removeWatcher(endpointId: String) {
        _otherWatchers = _otherWatchers.filter { it.endpointId != endpointId }
        if (_setupMode == SetupMode.HOST) {
            playbackSetupMessenger.sendAllWatchersUpdate()
        }
    }

    fun approveWatcher(watcher: Watcher) {
        setIsApprovedForWatcher(watcher, true)
        if (_setupMode == SetupMode.HOST) {
            playbackSetupMessenger.sendPlaybackSetupOptionsUpdate(to = watcher)
        }
    }

    fun disapproveWatcher(watcher: Watcher) {
        setIsApprovedForWatcher(watcher, false)
    }

    private fun setIsApprovedForWatcher(watcher: Watcher, isApproved: Boolean) {
        if (watcher.isApproved == isApproved) {
            return
        }
        val newWatcher = watcher.copy(isApproved = isApproved)
        val isMe = watcher.endpointId == _meAsWatcher.endpointId
        if (isMe) {
            if (_setupMode == SetupMode.HOST) {
                _playbackSetupState.update { it.copy(
                    hostAsWatcher = newWatcher,
                    meAsWatcher = newWatcher
                ) }
            } else {
                _meAsWatcher = newWatcher
            }
        } else {
            _otherWatchers = _otherWatchers.withReplacedWatcher(watcher, newWatcher)
        }
        if (_setupMode == SetupMode.HOST) {
            playbackSetupMessenger.sendAllWatchersUpdate()
        }
    }

    private fun List<Watcher>.withReplacedWatcher(
        watcher: Watcher,
        newValue: Watcher
    ): List<Watcher> {
        val watcherIndex = this.indexOf(watcher)
        if (watcherIndex == -1) {
            return this
        }
        return this.toMutableList().apply {
            this[watcherIndex] = newValue
        }
    }

    fun setVideofilesToPlayNoNotify(videofileNames: List<String>, startFrom: Int) {
        _playbackSetupOptions = _playbackSetupOptions.copy(
            videofileNames = videofileNames,
            selectedVideofileIndex = startFrom
        )
    }

    fun setVideofilesToPlayAndNotifyIfNeeded(videofileNames: List<String>, startFrom: Int) {
        _playbackSetupOptions = _playbackSetupOptions.copy(
            videofileNames = videofileNames,
            selectedVideofileIndex = startFrom
        )
        if (_setupMode == SetupMode.HOST) {
            playbackSetupMessenger.sendPlaybackSetupOptionsUpdate()
        }
    }

    fun setDoStream(doStream: Boolean) {
        _playbackSetupOptions = _playbackSetupOptions.copy(doStream = doStream)
        if (_setupMode == SetupMode.HOST) {
            playbackSetupMessenger.sendPlaybackSetupOptionsUpdate()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSetupOptions = _playbackSetupOptions.copy(playbackSpeed = speed)
        if (_setupMode == SetupMode.HOST) {
            playbackSetupMessenger.sendPlaybackSetupOptionsUpdate()
        }
    }

    fun setRepeatMode(repeatMode: @Player.RepeatMode Int) {
        _playbackSetupOptions = _playbackSetupOptions.copy(repeatMode = repeatMode)
        if (_setupMode == SetupMode.HOST) {
            playbackSetupMessenger.sendPlaybackSetupOptionsUpdate()
        }
    }

    fun sendOpenPlayer() {
        playbackSetupMessenger.sendOpenPlayer()
    }

    fun resetMessageEvents() {
        messageEvents = getEmptyMessageEvents()
    }

    private val playbackSetupMessenger = object {
        val messageConverter = JsonVisyncMessageConverter()

        fun process(msg: String, sender: RunningConnection) {
            val fullMessage = messageConverter.decode(msg)
            Log.d("shit", "videofilenames = ${_playbackSetupOptions.videofileNames}")

            when (fullMessage) {
                is RequestSelfInfoMessage -> {
                    sendRequestSelfInfoResponse(to = sender)
                }
                is SelfInfoMessage -> {
                    _meAsWatcher = _meAsWatcher.copy(
                        endpointId = fullMessage.endpointId,
                        username = fullMessage.username
                    )
                }
            }

            when (_setupMode) {
                SetupMode.HOST -> { processAsHost(fullMessage, sender) }
                SetupMode.GUEST -> { processAsGuest(fullMessage, sender) }
            }
        }

        private fun processAsHost(fullMessage: VisyncMessage, sender: RunningConnection) {
            when (fullMessage) {
                is SelfInfoMessage -> {
                    _hostAsWatcher = _meAsWatcher
                }
                is VersionMessage -> {
                    addWatcher(Watcher(
                        endpointId = sender.endpointId,
                        username = sender.username,
                        messagingVersion = fullMessage.version,
                        isApproved = false,
                        missingVideofileNames = listOf()
                    ))
                }
                is DoNotHaveVideofilesMessage -> {
                    val oldWatcher = _otherWatchers.find { it.endpointId == sender.endpointId }
                        ?: return
                    val newWatcher = oldWatcher.copy(
                        missingVideofileNames = fullMessage.videofileNames
                    )
                    _otherWatchers = _otherWatchers.withReplacedWatcher(oldWatcher, newWatcher)
                    sendAllWatchersUpdate(
                        to = _allRunningConnections
                            .filter { it.endpointId != sender.endpointId }
                    )
                }
            }
        }

        private fun processAsGuest(fullMessage: VisyncMessage, sender: RunningConnection) {
            when (fullMessage) {
                is RequestVersionMessage -> {
                    sendMessagingVersion(to = sender)
                }
                is PlaybackSetupOptionsUpdateMessage -> {
                    val oldPlaybackSetupOptions = _playbackSetupOptions
                    val oldVideofileNames = oldPlaybackSetupOptions.videofileNames
                    val oldSelectionIndex = oldPlaybackSetupOptions.selectedVideofileIndex
                    val newPlaybackSetupOptions = fullMessage.playbackSetupOptions
                    val newVideofileNames = newPlaybackSetupOptions.videofileNames
                    val newSelectionIndex = newPlaybackSetupOptions.selectedVideofileIndex
                    val shouldSetVideofiles = (
                        oldVideofileNames != newVideofileNames ||
                        oldSelectionIndex != newSelectionIndex
                    )
                    _playbackSetupOptions = fullMessage.playbackSetupOptions
                    Log.d("shit", "$oldVideofileNames $newVideofileNames")
                    if (shouldSetVideofiles) {
                        Log.d("shit", "onSetVideofiles is null = ${messageEvents.onSetVideofilesMessage == null}")
                        val missingFiles = messageEvents.onSetVideofilesMessage?.invoke(
                            newVideofileNames,
                            newSelectionIndex
                        ) ?: listOf()
                        _meAsWatcher = _meAsWatcher.copy(missingVideofileNames = missingFiles)
                        Log.d("shit", "missing files: $missingFiles")
                        sendDoNotHaveVideofiles(
                            videofileNames = missingFiles,
                            to = sender
                        )
                    }
                }
                is AllWatchersUpdateMessage -> {
                    val host = fullMessage.allWatchers
                        .find { it.endpointId == sender.endpointId }!!
                    val me = fullMessage.allWatchers
                        .find { it.endpointId == _meAsWatcher.endpointId }!!
                    val others = fullMessage.allWatchers
                        .filter { it.endpointId != _meAsWatcher.endpointId }
                    _playbackSetupState.update { it.copy(
                        hostAsWatcher = host,
                        meAsWatcher = me,
                        otherWatchers = others
                    ) }
                }
                is OpenPlayerMessage -> {
                    messageEvents.onOpenPlayerMessage?.invoke()
                }
                is PlaybackPauseUnpauseMessage -> {
                    messageEvents.onPauseUnpauseMessage?.invoke(fullMessage)
                }
            }
        }

        fun sendRequestSelfInfo(to: RunningConnection) {
            val requestSelfInfoMessage = RequestSelfInfoMessage()
            val msg = messageConverter.encode(requestSelfInfoMessage)
            to.sendMessage(msg)
        }

        private fun sendRequestSelfInfoResponse(to: RunningConnection) {
            val selfInfoMessage = SelfInfoMessage(
                endpointId = to.endpointId,
                username = to.username
            )
            val msg = messageConverter.encode(selfInfoMessage)
            to.sendMessage(msg)
        }

        private fun sendMessagingVersion(to: RunningConnection) {
            val versionMessage = VersionMessage(messagingVersion)
            val msg = messageConverter.encode(versionMessage)
            to.sendMessage(msg)
        }

        fun sendRequestMessagingVersion(to: RunningConnection) {
            val requestVersionMessage = RequestVersionMessage()
            val msg = messageConverter.encode(requestVersionMessage)
            to.sendMessage(msg)
        }

        fun sendAllWatchersUpdate(to: List<RunningConnection>) {
            val allWatchers = listOf(_meAsWatcher) + _otherWatchers
            val allWatchersUpdateMessage = AllWatchersUpdateMessage(allWatchers)
            val msg = messageConverter.encode(allWatchersUpdateMessage)
            connectionsAdvertiser.sendMessageToMultiple(
                msg = msg,
                receivers = to
            )
        }

        fun sendAllWatchersUpdate() {
            sendAllWatchersUpdate(to = _allRunningConnections)
        }

        fun sendPlaybackSetupOptionsUpdate(to: List<Watcher>) {
            val receiverIds = to.map { it.endpointId }
            val receivers = _allRunningConnections
                .filter { it.endpointId in receiverIds }
            val optionsUpdateMessage =  PlaybackSetupOptionsUpdateMessage(_playbackSetupOptions)
            val msg = messageConverter.encode(optionsUpdateMessage)
            connectionsAdvertiser.sendMessageToMultiple(msg, receivers)
        }

        fun sendPlaybackSetupOptionsUpdate(to: Watcher) {
            sendPlaybackSetupOptionsUpdate(to = listOf(to))
        }

        fun sendPlaybackSetupOptionsUpdate() {
            val approvedWatchers = _otherWatchers.filter { it.isApproved }
            sendPlaybackSetupOptionsUpdate(to = approvedWatchers)
        }

        private fun sendDoNotHaveVideofiles(videofileNames: List<String>, to: RunningConnection) {
            val doNotHaveVideofilesMessage = DoNotHaveVideofilesMessage(videofileNames)
            val msg = messageConverter.encode(doNotHaveVideofilesMessage)
            to.sendMessage(msg)
        }

        fun sendOpenPlayer() {
            val approvedWatcherIds = _otherWatchers
                .filter { it.isApproved }
                .map { it.endpointId }
            val receivers = _allRunningConnections
                .filter { it.endpointId in approvedWatcherIds }
            val openPlayerMsg = OpenPlayerMessage()
            val msg = messageConverter.encode(openPlayerMsg)
            connectionsAdvertiser.sendMessageToMultiple(msg, receivers)
        }
    }

    private fun getEmptyMessageEvents() = object : PlaybackSetupMessageEvents {
        override var onOpenPlayerMessage: (() -> Unit)? = null
        override var onSetVideofilesMessage: ((List<String>, Int) -> List<String>)? = null
        override var onPlaybackSetupOptionsUpdateMessage: ((PlaybackSetupOptions) -> Unit)? = null
        override var onPauseUnpauseMessage: ((PlaybackPauseUnpauseMessage) -> Unit)? = null
    }
}

data class PlaybackSetupState(
    val setupMode: SetupMode,
    val isConnectingToHost: Boolean,
    val connectionError: Boolean,
    val hostAsWatcher: Watcher,
    val meAsWatcher: Watcher,
    val otherWatchers: List<Watcher>,
    val playbackSetupOptions: PlaybackSetupOptions,
)


@Serializable
data class Watcher(
    val endpointId: String,
    val username: String,
    val messagingVersion: Int,
    val isApproved: Boolean,
    val missingVideofileNames: List<String>,
)

@Serializable
data class PlaybackSetupOptions(
    val videofileNames: List<String>,
    val selectedVideofileIndex: Int,
    val doStream: Boolean,
    val playbackSpeed: Float,
    val repeatMode: @Player.RepeatMode Int,
)

interface PlaybackSetupMessageEvents {
    var onOpenPlayerMessage: (() -> Unit)?
    var onSetVideofilesMessage: ((List<String>, Int) -> List<String>)?
    var onPlaybackSetupOptionsUpdateMessage: ((PlaybackSetupOptions) -> Unit)?
    var onPauseUnpauseMessage: ((PlaybackPauseUnpauseMessage) -> Unit)?
}

enum class SetupMode {
    HOST, GUEST
}