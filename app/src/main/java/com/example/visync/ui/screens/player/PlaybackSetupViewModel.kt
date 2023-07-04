package com.example.visync.ui.screens.player

import androidx.lifecycle.ViewModel
import com.example.visync.connections.ConnectionRequest
import com.example.visync.connections.RunningConnection
import com.example.visync.connections.VisyncNearbyConnections
import com.example.visync.connections.VisyncNearbyConnectionsListener
import com.example.visync.messaging.JsonVisyncMessageConverter
import com.example.visync.messaging.OpenPlayerMessage
import com.example.visync.messaging.RequestVersionMessage
import com.example.visync.messaging.AllWatchersUpdateMessage
import com.example.visync.messaging.RequestSelfInfoMessage
import com.example.visync.messaging.SelfInfoMessage
import com.example.visync.messaging.SetVideofilesMessage
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

    private val messagingVersion = 1
    private val setupModeDefault = SetupMode.HOST
    private val meAsWatcherDefault = Watcher(
        endpointId = "",
        username = "",
        messagingVersion = messagingVersion,
        isApproved = false
    )
    private val playbackSetupStateDefault = PlaybackSetupState(
        setupMode = setupModeDefault,
        isConnectingToHost = false,
        connectionError = false,
        hostAsWatcher = meAsWatcherDefault,
        meAsWatcher = meAsWatcherDefault,
        otherWatchers = listOf()
    )

    private val _playbackSetupState = MutableStateFlow(playbackSetupStateDefault)
    val playbackSetupState: StateFlow<PlaybackSetupState> = _playbackSetupState

    private var _setupMode
        get() = _playbackSetupState.value.setupMode
        set(value) = _playbackSetupState.update { state ->
            state.copy(
                setupMode = value
            )
        }
    private var _isConnectingToHost
        get() = _playbackSetupState.value.isConnectingToHost
        set(value) = _playbackSetupState.update { state ->
            state.copy(
                isConnectingToHost = value
            )
        }
    private var _connectionError
        get() = _playbackSetupState.value.connectionError
        set(value) = _playbackSetupState.update { state ->
            state.copy(
                connectionError = value
            )
        }
    private var _hostAsWatcher
        get() = _playbackSetupState.value.hostAsWatcher
        set(value) = _playbackSetupState.update { state ->
            state.copy(
                hostAsWatcher = value
            )
        }
    private var _meAsWatcher
        get() = _playbackSetupState.value.meAsWatcher
        set(value) = _playbackSetupState.update { state ->
            state.copy(
                meAsWatcher = value
            )
        }
    private var _otherWatchers
        get() = _playbackSetupState.value.otherWatchers
        set(value) = _playbackSetupState.update { state ->
            state.copy(
                otherWatchers = value
            )
        }

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
                    playbackSetupMessaging.sendRequestSelfInfo(connection)
                }
                if (_setupMode == SetupMode.HOST) {
                    playbackSetupMessaging.sendRequestMessagingVersion(connection)
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
                playbackSetupMessaging.process(message, from)
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
            playbackSetupMessaging.sendAllWatchersUpdate()
        }
    }

    private fun removeWatcher(endpointId: String) {
        _otherWatchers = _otherWatchers.filter { it.endpointId != endpointId }
        if (_setupMode == SetupMode.HOST) {
            playbackSetupMessaging.sendAllWatchersUpdate()
        }
    }

    fun approveWatcher(watcher: Watcher) {
        setIsApprovedForWatcher(watcher, true)
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
            playbackSetupMessaging.sendAllWatchersUpdate()
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

    fun sendOpenPlayer() {
        playbackSetupMessaging.sendOpenPlayer()
    }

    fun resetMessageEvents() {
        messageEvents = getEmptyMessageEvents()
    }

    private val playbackSetupMessaging = object {
        val messageConverter = JsonVisyncMessageConverter()

        fun process(msg: String, sender: RunningConnection) {
            val fullMessage = messageConverter.decode(msg)

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
                        isApproved = false
                    ))
                }
            }
        }

        private fun processAsGuest(fullMessage: VisyncMessage, sender: RunningConnection) {
            when (fullMessage) {
                is RequestVersionMessage -> {
                    sendMessagingVersion(to = sender)
                }
                is SetVideofilesMessage -> {
                    messageEvents.onSetVideofilesMessage?.invoke(fullMessage.videofileNames)
                }
                is AllWatchersUpdateMessage -> {
                    val host = fullMessage.allWatchers
                        .find { it.endpointId == sender.endpointId }!!
                    val others = fullMessage.allWatchers
                        .filter { it.endpointId != _meAsWatcher.endpointId }
                    _playbackSetupState.update { it.copy(
                        hostAsWatcher = host,
                        otherWatchers = others
                    ) }
                }
                is OpenPlayerMessage -> {
                    messageEvents.onOpenPlayerMessage?.invoke()
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

        fun sendAllWatchersUpdate() {
            val allWatchers = listOf(_meAsWatcher) + _otherWatchers
            val allWatchersUpdateMessage = AllWatchersUpdateMessage(allWatchers)
            val msg = messageConverter.encode(allWatchersUpdateMessage)
            connectionsAdvertiser.sendMessageToMultiple(
                msg = msg,
                receivers = connectionsAdvertiser.advertiserState.value.runningConnections
            )
        }

        fun sendOpenPlayer() {
            val approvedWatcherIds = _otherWatchers
                .filter { it.isApproved }
                .map { it.endpointId }
            val receivers = connectionsAdvertiser.advertiserState.value.runningConnections
                .filter { it.endpointId in approvedWatcherIds }
            val openPlayerMsg = OpenPlayerMessage()
            val msg = messageConverter.encode(openPlayerMsg)
            connectionsAdvertiser.sendMessageToMultiple(msg, receivers)
        }
    }

    private fun getEmptyMessageEvents() = object : PlaybackSetupMessageEvents {
        override var onOpenPlayerMessage: (() -> Unit)? = null
        override var onSetVideofilesMessage: ((List<String>) -> Unit)? = null
    }
}

data class PlaybackSetupState(
    val setupMode: SetupMode,
    val isConnectingToHost: Boolean,
    val connectionError: Boolean,
    val hostAsWatcher: Watcher,
    val meAsWatcher: Watcher,
    val otherWatchers: List<Watcher>,
)


@Serializable
data class Watcher(
    val endpointId: String,
    val username: String,
    val messagingVersion: Int,
    val isApproved: Boolean,
)

interface PlaybackSetupMessageEvents {
    var onOpenPlayerMessage: (() -> Unit)?
    var onSetVideofilesMessage: ((List<String>) -> Unit)?
}

enum class SetupMode {
    HOST, GUEST
}