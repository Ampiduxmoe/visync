package com.example.visync.ui.screens.main.playback_setup

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.visync.R
import com.example.visync.connections.BroadcastingState
import com.example.visync.connections.ConnectionRequest
import com.example.visync.connections.DiscoveredEndpoint
import com.example.visync.connections.RunningConnection
import com.example.visync.connections.VisyncNearbyConnections
import com.example.visync.connections.EmptyVisyncNearbyConnectionsListener
import com.example.visync.connections.VisyncNearbyConnectionsConfiguration
import com.example.visync.connections.VisyncNearbyConnectionsListener
import com.example.visync.data.videofiles.Videofile
import com.example.visync.messaging.AllWatchersUpdateMessage
import com.example.visync.messaging.CanNotRestoreYourConnectionMessage
import com.example.visync.messaging.ConnectionRestoredMessage
import com.example.visync.messaging.DevicePositionsMessage
import com.example.visync.messaging.DoNotHaveVideofilesMessage
import com.example.visync.messaging.JsonVisyncMessageConverter
import com.example.visync.messaging.MessengerVersionMessage
import com.example.visync.messaging.OpenPlayerMessage
import com.example.visync.messaging.PhysicalDeviceMessage
import com.example.visync.messaging.PingMessage
import com.example.visync.messaging.PlaybackOptionsUpdateMessage
import com.example.visync.messaging.PlaybackPauseUnpauseMessage
import com.example.visync.messaging.PongMessage
import com.example.visync.messaging.RequestMessengerVersionMessage
import com.example.visync.messaging.RequestOwnEndpointIdMessage
import com.example.visync.messaging.RequestPhysicalDeviceMessage
import com.example.visync.messaging.RestoreMyConnectionMessage
import com.example.visync.messaging.SelfWatcherUpdateMessage
import com.example.visync.messaging.SyncBallMessage
import com.example.visync.messaging.VisyncMessage
import com.example.visync.messaging.YourOwnEndpointIdMessage
import com.example.visync.metadata.VideoMetadata
import com.example.visync.primitives.VisyncColor
import com.example.visync.ui.screens.main.playback_setup.PingEntry.Companion.ZeroPingEntry
import com.example.visync.ui.screens.player.VisyncPhysicalDevice
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.lang.Long.min
import javax.inject.Inject
import kotlin.math.absoluteValue

/*
TODO: it is better to move connections functionality into separate connections manager class.
    So we would have nearby connections wrapper class managing raw connections
    and visync connections manager that knows some details of the app and keeps track of
    things like users online nickname and does initial communication (handshake) with others.
    It will be much more logical to build ping system around it that will be used by
    both player screen and playback setup screen and not how it is done now.
    Also code will be much cleaner in guest class where we have some
    connectivity state vars and checks interweaving with real guest viewer logic.
*/
@OptIn(FlowPreview::class)
@HiltViewModel
class PlaybackSetupViewModel @Inject constructor(
    visyncNearbyConnections: VisyncNearbyConnections,
    application: Application,
) : ViewModel() {
    private val _roleManager = PlaybackSetupRoleManager(
        visyncNearbyConnections = visyncNearbyConnections,
        application = application,
        coroutineScope = viewModelScope
    )

    var currentState = PlaybackSetupUserState.IDLE
        set(value) {
            if (!isInitialized) { throw Exception("PlaybackSetupViewModel was not initialized") }
            if (value == field) { return }
            when (value) {
                PlaybackSetupUserState.HOST -> {
                    _roleManager.transitionTo(_roleManager.playbackSetupHostState)
                }
                PlaybackSetupUserState.GUEST -> {
                    _roleManager.transitionTo(_roleManager.playbackSetupGuestState)
                }
                PlaybackSetupUserState.IDLE -> {
                    _roleManager.transitionTo(_roleManager.idleState)
                }
            }
            field = value
        }

    val hostPlaybackSetupState = _roleManager.playbackSetupHostState.playbackSetupState
        .debounce(STATE_DEBOUNCE_DELAY)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_SHARING_STOP_DELAY),
            initialValue = _roleManager.playbackSetupHostState.playbackSetupState.value
        )
    val hostDevicePositions = _roleManager.playbackSetupHostState.devicePositionsConfigurationState
    val hostConnectionState = _roleManager.playbackSetupHostState.hostConnectionState
        // better not to debounce this since if you have many ping receivers
        // you get bombarded with responses constantly and never update your state
        // solution is to update ping data every second or so
//        .debounce(PING_STATE_DEBOUNCE_DELAY)
//        .stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(STATE_SHARING_STOP_DELAY),
//            initialValue = _roleManager.playbackSetupHostState.hostConnectionState.value
//        )
    val hostActions: PlaybackSetupHostActions = _roleManager.playbackSetupHostState

    val guestPlaybackSetupState = _roleManager.playbackSetupGuestState.playbackSetupState
        .debounce(STATE_DEBOUNCE_DELAY)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_SHARING_STOP_DELAY),
            initialValue = _roleManager.playbackSetupGuestState.playbackSetupState.value
        )
    val guestDevicePositions = _roleManager.playbackSetupGuestState.devicePositionsConfigurationState
    val guestConnectionState = _roleManager.playbackSetupGuestState.guestConnectionState
    val guestActions: PlaybackSetupGuestActions = _roleManager.playbackSetupGuestState

    var commonCallbacks = _roleManager.playbackSetupHostState.playbackSetupCallbacks
        set(value) {
            if (value == field) { return }
            _roleManager.playbackSetupHostState.playbackSetupCallbacks = value
            _roleManager.playbackSetupGuestState.playbackSetupCallbacks = value
            field = value
        }
    var hostSpecificCallbacks = _roleManager.playbackSetupHostState.hostCallbacks
        set(value) {
            if (value == field) { return }
            _roleManager.playbackSetupHostState.hostCallbacks = value
            field = value
        }
    var guestSpecificCallbacks = _roleManager.playbackSetupGuestState.guestCallbacks
        set(value) {
            if (value == field) { return }
            _roleManager.playbackSetupGuestState.guestCallbacks = value
            field = value
        }

    var isInitialized = false
        private set
    fun initialize(config: PlaybackSetupViewModelConfiguration) {
        val playbackSetupStateConfig = PlaybackSetupConfiguration(
            username = config.username,
            physicalDevice = config.device
        )
        _roleManager.playbackSetupHostState.initialize(playbackSetupStateConfig)
        _roleManager.playbackSetupGuestState.initialize(playbackSetupStateConfig)
        commonCallbacks = config.commonPlaybackSetupCallbacks
        hostSpecificCallbacks = config.hostSpecificCallbacks
        guestSpecificCallbacks = config.guestSpecificCallbacks
        isInitialized = true
    }

    fun updatePhysicalDevice(device: VisyncPhysicalDevice) {
        _roleManager.playbackSetupHostState.setNewDevice(device)
        _roleManager.playbackSetupGuestState.setNewDevice(device)
    }

    fun setSelectedVideofiles(videofiles: List<Videofile>) {
        val currentRoleState = _roleManager.currentRoleState
        if (currentRoleState is PlaybackSetupRole) {
            currentRoleState.setSelectedVideofiles(videofiles)
        }
    }

    companion object {
        private const val STATE_SHARING_STOP_DELAY = 5000L
        private const val STATE_DEBOUNCE_DELAY = 50L
        private const val PING_STATE_DEBOUNCE_DELAY = 400L
    }
}

interface PlaybackSetupCommonActions {
    fun getPlaybackSetupSelfAsWatcher(): Watcher
    fun getPlaybackSetupHostAsWatcher(): Watcher
}

interface PlaybackSetupHostActions: PlaybackSetupCommonActions {
    fun startAdvertisingRoom()
    fun stopAdvertisingRoom()
    fun stopPinging()
    fun approveWatcher(watcher: Watcher)
    fun disapproveWatcher(watcher: Watcher)
    fun setSelectedVideofileIndex(index: Int)
    fun setPlaybackSpeed(playbackSpeed: Float)
    fun setRepeatMode(repeatMode: @Player.RepeatMode Int)
    fun saveDevicePositions(newValue: DevicePositionsEditor)
    fun sendOpenPlayer()
    suspend fun sendSyncBall(position: Offset, velocity: Offset)
}

interface PlaybackSetupGuestActions: PlaybackSetupCommonActions {
    fun startDiscoveringRooms()
    fun stopDiscoveringRooms()
    fun stopPonging()
    fun setGuestSpecificCallbacks(callbacks: GuestSpecificCallbacks)
}

data class PlaybackSetupViewModelConfiguration(
    val username: String,
    val device: VisyncPhysicalDevice,
    val commonPlaybackSetupCallbacks: PlaybackSetupCallbacks,
    val hostSpecificCallbacks: HostSpecificCallbacks,
    val guestSpecificCallbacks: GuestSpecificCallbacks
)

enum class PlaybackSetupUserState {
    HOST, GUEST, IDLE
}

data class PlaybackSetupState(
    val watchers: List<Watcher>,
    val playbackOptions: PlaybackOptions,
    val localSelectedVideofiles: List<Videofile>,
)


@Serializable
data class Watcher(
    val endpointId: String,
    val username: String,
    val messagingVersion: Int?,
    val physicalDevice: VisyncPhysicalDevice?,
    val isApproved: Boolean,
    val missingVideofileNames: List<String>, // TODO: should be ids, not names since names are not unique
) {
    val hasVersionMismatch: Boolean
        get() = messagingVersion != PlaybackSetupMessenger.VERSION

    val hasCompletedHandshake: Boolean
        get() {
            val hasMessagingVersion = messagingVersion != null
            val hasPhysicalDevice = physicalDevice != null
            return hasMessagingVersion && hasPhysicalDevice
        }

    val canBeApproved: Boolean
        get() {
            val matchesLocalVersion = messagingVersion == PlaybackSetupMessenger.VERSION
            val hasPhysicalDevice = physicalDevice != null
            val hasDimensionsOnDevice = physicalDevice != VisyncPhysicalDevice.NoDimensionsDevice
            val hasAllVideofiles = missingVideofileNames.isEmpty()
            return matchesLocalVersion && hasPhysicalDevice && hasDimensionsOnDevice && hasAllVideofiles
        }
}

@Serializable
data class PlaybackOptions(
    val videofilesMetadata: List<VideoMetadata>,
    val selectedVideofileIndex: Int,
    val playbackSpeed: Float,
    val repeatMode: @Player.RepeatMode Int,
)

interface PlaybackSetupMessageEvents {
    var onOpenPlayerMessage: (() -> Unit)?
    var onPauseUnpauseMessage: ((PlaybackPauseUnpauseMessage) -> Unit)?
}

// TODO: maybe add a timer to callback actions and do something like disconnect if no actions were done by user
private class PlaybackSetupRoleManager(
    visyncNearbyConnections: VisyncNearbyConnections,
    application: Application,
    coroutineScope: CoroutineScope
) {

    val playbackSetupHostState: HostState = HostState(visyncNearbyConnections, application, coroutineScope)
    val playbackSetupGuestState: GuestState = GuestState(visyncNearbyConnections)
    val idleState: IdleState = IdleState()

    var currentRoleState: RoleState = idleState

    fun transitionTo(role: RoleState) {
        Log.d("roles", "transitioning to")
        val validRoleStates = listOf(playbackSetupHostState, playbackSetupGuestState, idleState)
        if (role !in validRoleStates) { throw IllegalArgumentException() }
        if (role == currentRoleState) { return }
        when (role) {
            playbackSetupHostState -> {
                Log.d("roles", "to host")
            }
            playbackSetupGuestState -> {
                Log.d("roles", "to guest")
            }
            idleState -> {
                Log.d("roles", "to idle")
            }
        }
        currentRoleState.exitRole()
        role.enterRole()
        currentRoleState = role
    }
}

abstract class PlaybackSetupRole(
    protected val nearbyConnections: VisyncNearbyConnections,
): PlaybackSetupCommonActions {

    protected val _defaultWatcher = Watcher(
        endpointId = "",
        username = "",
        messagingVersion = null,
        isApproved = false,
        missingVideofileNames = emptyList(),
        physicalDevice = null
    )

    private val _defaultPlaybackOptions = PlaybackOptions(
        videofilesMetadata = emptyList(),
        selectedVideofileIndex = 0,
        playbackSpeed = 1f,
        repeatMode = Player.REPEAT_MODE_OFF
    )

    private val _defaultPlaybackSetupState = PlaybackSetupState(
        watchers = listOf(
            _defaultWatcher.copy(
                messagingVersion = PlaybackSetupMessenger.VERSION
            ) // self watcher
        ),
        playbackOptions = _defaultPlaybackOptions,
        localSelectedVideofiles = emptyList()
    )

    private val _defaultPositionsConfigurationState: DevicePositionsEditor? = null

    private val _mainMutableStateFlow = MutableStateFlow(_defaultPlaybackSetupState)
    val playbackSetupState = _mainMutableStateFlow.asStateFlow()

    private val _positionsMutableStateFlow = MutableStateFlow(_defaultPositionsConfigurationState)
    val devicePositionsConfigurationState = _positionsMutableStateFlow.asStateFlow()

    private var _playbackSetupState
        get() = _mainMutableStateFlow.value
        private set(value) { _mainMutableStateFlow.value = value }

    private var _positionsState
        get() = _positionsMutableStateFlow.value
        private set(value) { _positionsMutableStateFlow.value = value }

    protected var allWatchers
        get() = _playbackSetupState.watchers
        private set(value) { _mainMutableStateFlow.update { it.copy(watchers = value) } }

    protected var selfWatcher = allWatchers[0]
        protected set(value) {
            if (field == value) { return }
            allWatchers = listOf(value) + otherWatchers
            field = value
        }

    protected var otherWatchers
        get() = allWatchers.filter { it !== selfWatcher }
        protected set(value) {
            if (otherWatchers == value) { return }
            allWatchers = listOf(selfWatcher) + value
        }

    protected var playbackOptions
        get() = _playbackSetupState.playbackOptions
        protected set(value) {
            _mainMutableStateFlow.update { it.copy(playbackOptions = value) }
        }

    protected var localSelectedFiles
        get() = _playbackSetupState.localSelectedVideofiles
        protected set(value) {
            _mainMutableStateFlow.update { it.copy(localSelectedVideofiles = value) }
        }

    protected var positionsEditor
        get() = _positionsState
        set(value) { _positionsState = value }

    var playbackSetupCallbacks: PlaybackSetupCallbacks = EmptyPlaybackSetupCallbacks()

    protected val messageConverter = JsonVisyncMessageConverter()

    protected abstract val visyncNearbyConnectionsListener: VisyncNearbyConnectionsListener

    protected abstract fun notifySelfChange()

    protected fun startListeningToVisyncNearbyConnections() {
        if (!isInitialized()) { throw Exception("Object was not initialized properly") }
        nearbyConnections.addEventListener(visyncNearbyConnectionsListener)
    }

    protected fun stopListeningToVisyncNearbyConnections() {
        if (!isInitialized()) { throw Exception("Object was not initialized properly") }
        nearbyConnections.removeEventListener(visyncNearbyConnectionsListener)
    }

    protected fun commonOnAcceptTaskFailure(
        endpointInfo: EndpointInfo,
        exception: Exception,
        request: ConnectionRequest,
    ) {
        playbackSetupCallbacks.connectionAcceptTaskFailure(
            endpointInfo = endpointInfo,
            exception = exception,
            actions = object : ConnectionAcceptTaskFailureActions {
                override fun tryAgain(onRetrySuccess: () -> Unit) {
                    request.accept(
                        onTaskFailure = { newException ->
                            commonOnAcceptTaskFailure(endpointInfo, newException, request)
                        },
                        onTaskSuccess = {
                            commonOnAcceptTaskSuccess(endpointInfo)
                            onRetrySuccess()
                        },
                        onConnectionResult = { result ->
                            commonOnAcceptConnectionResult(endpointInfo, result)
                        }
                    )
                }
                override fun rejectConnection() {
                    request.reject(
                        onTaskFailure = { exception ->
                            commonOnRejectTaskFailure(endpointInfo, exception, request)
                        },
                        onTaskSuccess = {
                            commonOnRejectTaskSuccess(endpointInfo)
                        },
                    )
                }
            }
        )
    }
    protected fun commonOnRejectTaskFailure(
        endpointInfo: EndpointInfo,
        exception: Exception,
        request: ConnectionRequest,
    ) {
        playbackSetupCallbacks.connectionRejectTaskFailure(
            endpointInfo = endpointInfo,
            exception = exception,
            actions = object : ConnectionRejectTaskFailureActions {
                override fun tryAgain(onRetrySuccess: () -> Unit) {
                    request.reject(
                        onTaskFailure = { newException ->
                            commonOnRejectTaskFailure(endpointInfo, newException, request)
                        },
                        onTaskSuccess = {
                            commonOnRejectTaskSuccess(endpointInfo)
                            onRetrySuccess()
                        },
                    )
                }
            }
        )
    }
    protected fun commonOnAcceptTaskSuccess(
        endpointInfo: EndpointInfo,
    ) {
        playbackSetupCallbacks.connectionAcceptTaskSuccess(
            endpointInfo = endpointInfo
        )
    }
    protected fun commonOnRejectTaskSuccess(
        endpointInfo: EndpointInfo
    ) {
        playbackSetupCallbacks.connectionRejectTaskSuccess(
            endpointInfo = endpointInfo
        )
    }
    protected fun commonOnAcceptConnectionResult(
        endpointInfo: EndpointInfo,
        result: ConnectionResolution
    ) {
        when (result.status.statusCode) {
            ConnectionsStatusCodes.STATUS_OK -> {
                playbackSetupCallbacks.connectionAcceptResultAccepted(
                    endpointInfo = endpointInfo,
                )
            }
            ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                playbackSetupCallbacks.connectionAcceptResultRejected(
                    endpointInfo = endpointInfo,
                )
            }
            else -> {
                playbackSetupCallbacks.connectionAcceptResultUnresolvedStatusCode(
                    endpointInfo = endpointInfo,
                    result = result
                )
            }
        }

    }
    protected fun commonOnCommunicationFailure(
        watcher: Watcher,
        runningConnection: RunningConnection,
        msg: String,
        exception: Exception,
    ) {
        playbackSetupCallbacks.onCommunicationFailure(
            watcher = watcher,
            exception = exception,
            actions = object : CommunicationFailureActions {
                override fun tryAgain(onRetrySuccess: () -> Unit) {
                    runningConnection.sendMessage(
                        msg = msg,
                        onTaskFailure = { newException ->
                            commonOnCommunicationFailure(watcher, runningConnection, msg, newException)
                        },
                        onTaskSuccess = onRetrySuccess
                    )
                }
                override fun dropConnection() {
                    runningConnection.disconnect()
                }
            }
        )
    }

    fun initialize(config: PlaybackSetupConfiguration) {
        val connectionsState = nearbyConnections.connectionsState.value
        val isBroadcasting = connectionsState.broadcastingState != BroadcastingState.IDLE
        if (isBroadcasting) { throw Exception("Can't reinitialize during broadcasting") }
        selfWatcher = selfWatcher.copy(
            username = config.username,
            physicalDevice = config.physicalDevice
        )
        nearbyConnections.initialize(
            VisyncNearbyConnectionsConfiguration(
                username = config.username
            )
        )
    }

    /** Sets all states to default, but does not reset username and physical device */
    open fun resetToDefaultState() {
        selfWatcher = _defaultWatcher.copy(
            username = selfWatcher.username,
            messagingVersion = PlaybackSetupMessenger.VERSION,
            physicalDevice = selfWatcher.physicalDevice
        )
        _mainMutableStateFlow.value = _defaultPlaybackSetupState.copy(
            watchers = listOf(selfWatcher)
        )
        _positionsMutableStateFlow.value = _defaultPositionsConfigurationState
    }

    fun setNewDevice(device: VisyncPhysicalDevice) {
        if (selfWatcher.physicalDevice == device) { return }
        selfWatcher = selfWatcher.copy(physicalDevice = device)
        notifySelfChange()
    }

    abstract fun setSelectedVideofiles(videofiles: List<Videofile>)

    private fun isInitialized(): Boolean {
        val hasUsername = selfWatcher.username != _defaultWatcher.username
        val hasPhysicalDevice = selfWatcher.physicalDevice != _defaultWatcher.physicalDevice
        return hasUsername && hasPhysicalDevice
    }

    override fun getPlaybackSetupSelfAsWatcher(): Watcher {
        return selfWatcher
    }
}

abstract class PlaybackSetupHost(
    nearbyConnections: VisyncNearbyConnections,
    private val application: Application,
): PlaybackSetupRole(nearbyConnections), PlaybackSetupHostActions {

    private val _defaultHostConnectionState = HostConnectionState(
        isAdvertising = false,
        allWatcherPings = emptyList()
    )

    private val _hostMutableStateFlow = MutableStateFlow(_defaultHostConnectionState)
    val hostConnectionState = _hostMutableStateFlow.asStateFlow()

    private var _watcherPings
        get() = _hostMutableStateFlow.value.allWatcherPings
        set(value) = _hostMutableStateFlow.update { it.copy(allWatcherPings = value) }

    private var restoreConnectionWaitingList = mutableListOf<RestoreConnectionEntry>()

    var hostCallbacks: HostSpecificCallbacks = EmptyHostSpecificCallbacks()

    private var _onlyRestoreConnections = false

    override val visyncNearbyConnectionsListener: VisyncNearbyConnectionsListener =
        object : EmptyVisyncNearbyConnectionsListener() {
            override fun onBroadcastingStateChanged(newState: BroadcastingState) {
                _hostMutableStateFlow.update { it.copy(
                    isAdvertising = newState == BroadcastingState.ADVERTISING
                ) }
            }
            override fun onNewConnectionRequest(request: ConnectionRequest) {
                val endpointInfo = EndpointInfo(request)
                request.accept(
                    onTaskFailure = { exception ->
                        commonOnAcceptTaskFailure(
                            endpointInfo = endpointInfo,
                            exception = exception,
                            request = request
                        )
                    },
                    onTaskSuccess = {
                        commonOnAcceptTaskSuccess(
                            endpointInfo = endpointInfo
                        )
                    },
                    onConnectionResult = { result ->
                        commonOnAcceptConnectionResult(
                            endpointInfo = endpointInfo,
                            result = result
                        )
                    }
                )
            }
            override fun onNewRunningConnection(connection: RunningConnection) {
                val newWatcher = _defaultWatcher.copy(
                    endpointId = connection.endpointId,
                    username = connection.username
                )
                otherWatchers = otherWatchers + newWatcher
                _watcherPings = _watcherPings + EndpointPingData(
                    endpointId = newWatcher.endpointId,
                    pingData = PingData()
                )
                messenger.requestOwnEndpointId(from = connection)
            }
            override fun onRunningConnectionLost(connection: RunningConnection) {
                otherWatchers.find { it.endpointId == connection.endpointId }?.let { watcher ->
                    onConnectionLost(watcher)
                    return
                }

                val warningMsg = (
                    "Could not find watcher associated with given connection. " +
                    "`Connection lost` callback will not be invoked."
                )
                Log.w(PlaybackSetupHost::class.simpleName, warningMsg)
            }
            override fun onNewMessage(message: String, from: RunningConnection) {
                val fullMessage = messageConverter.decode(message)
                messenger.processMessage(fullMessage, from)
            }
        }

    override fun getPlaybackSetupHostAsWatcher(): Watcher {
        return selfWatcher
    }

    override fun startAdvertisingRoom() {
        nearbyConnections.startAdvertising(
            onTaskFailure = { exception ->
                hostCallbacks.onStartAdvertisingFailed(exception)
            },
            onTaskSuccess = {
                selfWatcher = selfWatcher.copy(endpointId = _defaultWatcher.endpointId)
                _onlyRestoreConnections = false
            }
        )
    }

    override fun stopAdvertisingRoom() {
        nearbyConnections.stopAdvertising()
        _onlyRestoreConnections = true
    }

    override fun approveWatcher(watcher: Watcher) {
        setIsApproved(watcher, true)
    }

    override fun disapproveWatcher(watcher: Watcher) {
        setIsApproved(watcher, false)
    }

    override fun setSelectedVideofileIndex(index: Int) {
        playbackOptions = playbackOptions.copy(
            selectedVideofileIndex = index
        )
        messenger.sendPlaybackOptionsUpdate()
    }

    override fun setPlaybackSpeed(playbackSpeed: Float) {
        playbackOptions = playbackOptions.copy(
            playbackSpeed = playbackSpeed
        )
        messenger.sendPlaybackOptionsUpdate()
    }

    override fun setRepeatMode(repeatMode: @Player.RepeatMode Int) {
        playbackOptions = playbackOptions.copy(
            repeatMode = repeatMode
        )
        messenger.sendPlaybackOptionsUpdate()
    }

    override fun sendOpenPlayer() {
        messenger.sendOpenPlayer()
    }

    override suspend fun sendSyncBall(position: Offset, velocity: Offset) {
        Log.d("syncBall", "entered")
        val approvedEndpointIds = allWatchers.filter { it.isApproved }.map { it.endpointId }
        val pingsCopy = _watcherPings.filter { it.endpointId in approvedEndpointIds }
        val halfOfMaxPing = pingsCopy.maxOfOrNull { it.pingData.weightedAverage } ?: return
        Log.d("syncBall", "halfOfMaxPing = $halfOfMaxPing")
        val fullMessage = SyncBallMessage(
            posX = position.x,
            posY = position.y,
            velocityX = velocity.x,
            velocityY = velocity.y,
            pingData = pingsCopy
        )
        val msg = messageConverter.encode(fullMessage)
        nearbyConnections.sendMessageToMultiple(
            msg = msg,
            endpointIds = approvedEndpointIds
        )
        delay(halfOfMaxPing.toLong())
    }

    private fun setIsApproved(watcher: Watcher, newValue: Boolean) {
        if (watcher.isApproved == newValue) { return }
        if (!watcher.canBeApproved) { return }
        val newWatcher = watcher.copy(
            isApproved = newValue
        )
        if (watcher === selfWatcher) {
            selfWatcher = newWatcher
            notifySelfChange()
        } else {
            otherWatchers = otherWatchers.withReplacedValueFoundByReference(
                value = watcher,
                newValue = newWatcher
            )
            messenger.sendAllWatchersUpdate()
        }
        positionsEditor = if (newValue) {
            positionsEditor?.withAddedWatcher(
                watcher = newWatcher
            )
        } else {
            positionsEditor?.withRemovedWatcher(
                watcher = newWatcher
            )
        }
    }

    private var _pingJob: Job? = null

    fun startPinging(
        coroutineScope: CoroutineScope,
        pingInterval: Long,
        cycleInterval: Long
    ) {
        if (_pingJob != null) { return }
        _pingJob = coroutineScope.launch {
            while (true) {
                val runningConnections = nearbyConnections.connectionsState.value.runningConnections
                launch {
                    for (connection in runningConnections) {
                        messenger.sendPing(to = connection)
                        delay(cycleInterval)
                    }
                }
                delay(pingInterval)
            }
        }
    }

    override fun stopPinging() {
        _pingJob?.cancel()
        _pingJob = null
    }

    override fun notifySelfChange() {
        val fullMessage = AllWatchersUpdateMessage(
            allWatchers = allWatchers,
            timestamp = getCurrentTimestamp()
        )
        messenger.encodeAndSendToWatchers(
            fullMessage = fullMessage,
            filter = { hasCompletedHandshake }
        )
    }

    override fun resetToDefaultState() {
        super.resetToDefaultState()
        _hostMutableStateFlow.value = _defaultHostConnectionState
    }

    override fun setSelectedVideofiles(videofiles: List<Videofile>) {
        localSelectedFiles = videofiles
        playbackOptions = playbackOptions.copy(
            videofilesMetadata = videofiles.map { it.metadata }
        )
        if (videofiles.isEmpty()) {
            positionsEditor = null
        } else {
            positionsEditor = positionsEditor?.withReplacedVideo(
                videofile = videofiles.first()
            ) ?: DevicePositionsEditor.create(
                watchers = allWatchers.filter { it.isApproved },
                videofile = videofiles.first(),
            )
        }
        messenger.sendPlaybackOptionsUpdate()
    }

    override fun saveDevicePositions(newValue: DevicePositionsEditor) {
        positionsEditor = newValue
        messenger.sendDevicePositionsUpdate()
    }

    private val messenger = object : PlaybackSetupMessenger() {
        override fun processMessage(fullMessage: VisyncMessage, sender: RunningConnection) {
            /*  Default flow should be like this (steps marked with '!' should not ever change):
                (0!) New running connection = new watcher with known endpoint id and username -> (1)
                (1!) Request own endpoint id -> receive endpoint id and update self if needed -> (2)
                (2!) Request their messenger version -> receive version and update their watcher, and if versions match then (3)
                (3)  Request their physical device -> receive it and update associated watcher -> (-)
                (-)  Handshake done -> send all watchers update, playback options update
                     and add their device to positions editor.

                Alternative flow when attempting to restore lost connection
                (0!) Running connection lost -> watcher removed from watchers list -> wait for a new running connection -> (1)
                (1!) Request own endpoint id (default path) -> do not receive endpoint id but connection restore request
                     (since endpoint was able to connect to us we assume our endpoint id didn't change) -> (2)
                (2)  Invoke on connection restore request callback:
                         if user calls `restore` function -> (3)
                         if user calls `refuse` function -> (4)
                (3)  Restore watcher state and send connection restored message -> END
                (4)  Send can not restore connection message -> END

                Other messages do not always follow original handshake sequence and can be sent at any moment.

                What we send:
                * All watchers update: is used to send any updates on guests or self
                * Playback options update: is used to send changes
                  in playback speed, selected videofiles, etc
                  CAN RECEIVE A REPLY containing missing videofiles
                * Ping: when handshake is done watcher can be put into ping list
                   SHOULD RECEIVE A REPLY of pong type

                What guests send (on their own):
                * Request own endpoint id: we just reply with their endpoint id */

            when (fullMessage) {
                is RequestOwnEndpointIdMessage -> {
                    replyWithTheirEndpointId(to = sender)
                }
                is YourOwnEndpointIdMessage -> {
                    selfWatcher = selfWatcher.copy(endpointId = fullMessage.endpointId)
                    if (_onlyRestoreConnections) {
                        // sender wanted to do default handshake, but we only restore sessions currently
                        sender.disconnect()
                        return
                    }
                    requestMessagingVersion(requestFrom = sender)
                }
                is MessengerVersionMessage -> {
                    val senderWatcher = sender.associatedWatcher()!!
                    val newWatcher = senderWatcher.copy(
                        messagingVersion = fullMessage.messagingVersion
                    )
                    otherWatchers = otherWatchers.withReplacedValueFoundByReference(
                        value = senderWatcher,
                        newValue = newWatcher
                    )

                    if (!newWatcher.hasVersionMismatch) {
                        requestPhysicalDevice(requestFrom = sender)
                    }
                }
                is PhysicalDeviceMessage -> {
                    val senderWatcher = sender.associatedWatcher()!!
                    val newWatcher = senderWatcher.copy(
                        physicalDevice = fullMessage.physicalDevice
                    )
                    otherWatchers = otherWatchers.withReplacedValueFoundByReference(
                        value = senderWatcher,
                        newValue = newWatcher
                    )
                    /*  Handshake completed, sender should now see other watchers,
                        other watchers should see him */
                    sendAllWatchersUpdate()
                    sendPlaybackOptionsUpdate(to = sender)
                }
                is DoNotHaveVideofilesMessage -> {
                    val senderWatcher = sender.associatedWatcher()!!
                    val newWatcher = senderWatcher.copy(
                        missingVideofileNames = fullMessage.videofileNames
                    )
                    otherWatchers = otherWatchers.withReplacedValueFoundByReference(
                        value = senderWatcher,
                        newValue = newWatcher
                    )
                    sendAllWatchersUpdate(
                        receiversFilter = { endpointId != sender.endpointId }
                    )
                }
                is RestoreMyConnectionMessage -> {
                    restoreConnectionWaitingList.find {
                        it.watcher.endpointId == sender.endpointId
                    }?.let { restoreEntry ->
                        hostCallbacks.onConnectionRestoreRequest(
                            restoreEntry = restoreEntry,
                            actions = object : RestoreConnectionRequestActions {
                                override fun restoreConnection() {
                                    val oldWatcher = otherWatchers
                                        .find { it.endpointId == sender.endpointId }!!
                                    val newWatcher = restoreEntry.watcher.copy(
                                        endpointId = sender.endpointId,
                                        username = sender.username
                                    )
                                    otherWatchers = otherWatchers.withReplacedValueFoundByReference(
                                        value = oldWatcher,
                                        newValue = newWatcher
                                    )
                                    val messageToSend = ConnectionRestoredMessage(
                                        allWatchers = allWatchers,
                                        playbackOptions = playbackOptions,
                                        positions = positionsEditor
                                    )
                                    encodeAndSend(messageToSend, sender)
                                }
                                override fun refuseToRestoreConnection() {
                                    val messageToSend = CanNotRestoreYourConnectionMessage(
                                        reason = application.getString(
                                            R.string.connection_restore_denied_by_host
                                        )
                                    )
                                    encodeAndSend(messageToSend, sender)
                                }
                            }
                        )
                        restoreConnectionWaitingList -= restoreEntry
                        return
                    }
                    val messageToSend = CanNotRestoreYourConnectionMessage(
                        reason = application.getString(
                            R.string.connection_restore_nothing_to_restore
                        )
                    )
                    encodeAndSend(messageToSend, sender)
                }
                is PongMessage -> {
                    val pongTimestamp = getCurrentTimestamp()
                    val oldWatcherPingsEntry = _watcherPings
                        .find { it.endpointId == sender.endpointId } ?: return
                    val oldPingEntry = oldWatcherPingsEntry.pingData.pingList
                        .find { it.requestTimestamp == fullMessage.pingTimestamp } ?: return
                    val newPingEntry = oldPingEntry.copy(
                        responseTimestamp = pongTimestamp
                    )
                    val newWatcherPingsEntry = oldWatcherPingsEntry.copy(
                        pingData = oldWatcherPingsEntry.pingData.withReplacedEntry(
                            entry = oldPingEntry,
                            newEntry = newPingEntry
                        )
                    )
                    _hostMutableStateFlow.update { it.copy(
                        allWatcherPings = it.allWatcherPings.withReplacedValueFoundByReference(
                            value = oldWatcherPingsEntry,
                            newValue = newWatcherPingsEntry
                        )
                    ) }
                }
            }
        }
    }
    private inline fun <reified T: VisyncMessage> PlaybackSetupMessenger.encodeAndSend(
        fullMessage: T,
        to: RunningConnection
    ) {
        val msg = messageConverter.encode(fullMessage)
        to.sendMessage(
            msg = msg,
            onTaskFailure = { exception ->
                commonOnCommunicationFailure(
                    watcher = to.associatedWatcher()!!,
                    runningConnection = to,
                    msg = msg,
                    exception = exception
                )
            }
        )
    }
    private inline fun <reified T: VisyncMessage> PlaybackSetupMessenger.encodeAndSendToWatchers(
        fullMessage: T,
        filter: Watcher.() -> Boolean
    ) {
        val msg = messageConverter.encode(fullMessage)
        val receivers = otherWatchers.filter { it.filter() }.map { it.endpointId }
        nearbyConnections.sendMessageToMultiple(
            msg = msg,
            endpointIds = receivers,
            onTaskFailure = { exception ->
                onMassCommunicationFailure(
                    endpointIds = receivers,
                    msg = msg,
                    exception = exception
                )
            }
        )
    }
    private fun PlaybackSetupMessenger.replyWithTheirEndpointId(
        to: RunningConnection,
    ) {
        val fullMessage = YourOwnEndpointIdMessage(to.endpointId)
        encodeAndSend(fullMessage, to)
    }
    private fun PlaybackSetupMessenger.requestOwnEndpointId(
        from: RunningConnection,
    ) {
        val fullMessage = RequestOwnEndpointIdMessage()
        encodeAndSend(fullMessage, from)
    }
    private fun PlaybackSetupMessenger.requestMessagingVersion(
        requestFrom: RunningConnection,
    ) {
        val fullMessage = RequestMessengerVersionMessage()
        encodeAndSend(fullMessage, requestFrom)
    }
    private fun PlaybackSetupMessenger.sendAllWatchersUpdate(
        receiversFilter: (Watcher.() -> Boolean)? = null,
    ) {
        val fullMessage = AllWatchersUpdateMessage(
            allWatchers = allWatchers.filter { it.hasCompletedHandshake },
            timestamp = getCurrentTimestamp()
        )
        val finalFilter: (Watcher.() -> Boolean) = when (receiversFilter) {
            null -> { { hasCompletedHandshake } }
            else -> { { hasCompletedHandshake && receiversFilter() } }
        }
        encodeAndSendToWatchers(
            fullMessage = fullMessage,
            filter = finalFilter
        )
    }
    private fun PlaybackSetupMessenger.sendPlaybackOptionsUpdate(
        to: RunningConnection? = null
    ) {
        val fullMessage = PlaybackOptionsUpdateMessage(
            playbackOptions = playbackOptions,
            timestamp = getCurrentTimestamp()
        )
        if (to != null) {
            encodeAndSend(fullMessage, to)
        } else {
            encodeAndSendToWatchers(
                fullMessage = fullMessage,
                filter = { hasCompletedHandshake }
            )
        }
    }
    private fun PlaybackSetupMessenger.requestPhysicalDevice(
        requestFrom: RunningConnection,
    ) {
        val fullMessage = RequestPhysicalDeviceMessage()
        encodeAndSend(fullMessage, requestFrom)
    }
    private fun PlaybackSetupMessenger.sendOpenPlayer() {
        val fullMessage = OpenPlayerMessage()
        encodeAndSendToWatchers(
            fullMessage = fullMessage,
            filter = { isApproved }
        )
    }
    private fun PlaybackSetupMessenger.sendPing(
        to: RunningConnection
    ) {
        val currentTimestamp = getCurrentTimestamp()
        val fullMessage = PingMessage(
            pingTimestamp = currentTimestamp
        )
        val msg = messageConverter.encode(fullMessage)
        to.sendMessage(msg)

        val oldWatcherPingsEntry = _watcherPings
            .find { it.endpointId == to.endpointId } ?: return
        _watcherPings = _watcherPings.withReplacedValueFoundByReference(
            value = oldWatcherPingsEntry,
            newValue = oldWatcherPingsEntry.withUnansweredPing(
                pingTimestamp = currentTimestamp
            )
        )
    }
    private fun PlaybackSetupMessenger.sendDevicePositionsUpdate() {
        val fullMessage = DevicePositionsMessage(positionsEditor!!)
        encodeAndSendToWatchers(
            fullMessage = fullMessage,
            filter = { hasCompletedHandshake }
        )
    }

    private fun onConnectionLost(
        watcher: Watcher,
    ) {
        otherWatchers = otherWatchers.filter { it !== watcher }
        _watcherPings = _watcherPings.filter { it.endpointId != watcher.endpointId }
        if (!watcher.hasCompletedHandshake) { return }
        restoreConnectionWaitingList.add(
            RestoreConnectionEntry(
                watcher = watcher,
                deviceOnEditor = positionsEditor?.devicesOnEditor
                    ?.find { it.watcherInfo.endpointId == watcher.endpointId }
            )
        )
        val restoreListLength = restoreConnectionWaitingList.count()
        if (restoreListLength > 40) {
            restoreConnectionWaitingList = restoreConnectionWaitingList.subList(
                fromIndex = restoreListLength / 2,
                toIndex = restoreListLength
            )
        }
    }

    private fun onMassCommunicationFailure(
        endpointIds: List<String>,
        msg: String,
        exception: Exception
    ) {
        hostCallbacks.onMassCommunicationFailure(
            exception = exception,
            actions = object : MassCommunicationFailureActions {
                override fun tryAgain(onRetrySuccess: () -> Unit) {
                    nearbyConnections.sendMessageToMultiple(
                        msg = msg,
                        endpointIds = endpointIds,
                        onTaskFailure = { newException ->
                            onMassCommunicationFailure(endpointIds, msg, newException)
                        },
                        onTaskSuccess = onRetrySuccess
                    )
                }
                override fun dropAllConnections() {
                    nearbyConnections.resetToIdle()
                }
            }
        )
    }

    private fun RunningConnection.associatedWatcher(): Watcher? {
        return otherWatchers.find { it.endpointId == endpointId }
    }

    companion object {
        val DefaultPingingInterval = 8000L
        val DefaultPingCyclingInterval = 200L
    }
}

abstract class PlaybackSetupGuest(
    nearbyConnections: VisyncNearbyConnections,
): PlaybackSetupRole(nearbyConnections), PlaybackSetupGuestActions {

    private val _defaultGuestConnectionState = GuestConnectionState(
        connectionStatus = GuestConnectionStatus.IDLE,
        discoveredRooms = emptyList()
    )

    private val _guestMutableStateFlow = MutableStateFlow(_defaultGuestConnectionState)
    val guestConnectionState = _guestMutableStateFlow.asStateFlow()

    private var _connectionStatus
        get() = _guestMutableStateFlow.value.connectionStatus
        set(value) {
            if (value == _connectionStatus) { return }
            _guestMutableStateFlow.update { it.copy(connectionStatus = value) }
        }

    private var _discoveredRooms
        get() = _guestMutableStateFlow.value.discoveredRooms
        set(value) {
            if (value == _discoveredRooms) { return }
            _guestMutableStateFlow.update { it.copy(discoveredRooms = value) }
        }

    private val _hostConnection: RunningConnection?
        get() = nearbyConnections.connectionsState.value.runningConnections.firstOrNull()

    val hostWatcher: Watcher?
        get() {
            if (_connectionStatus != GuestConnectionStatus.CONNECTED) { return null }
            return otherWatchers.find { it.endpointId == _hostConnection!!.endpointId }
        }

    var guestCallbacks: GuestSpecificCallbacks = EmptyGuestSpecificCallbacks()

    private var _isInConnectionRestoreMode = false
    private var _oldEndpointId = ""
    private var _canAcceptNewConnections = false
    private var _canCallbackChangeConnectionStatus = true

    private var _latestWatchersUpdateTimestamp = 0L
    private var _latestPlaybackOptionsUpdateTimestamp = 0L
    private var _latestDeviceConfigUpdateTimestamp = 0L

    override val visyncNearbyConnectionsListener: VisyncNearbyConnectionsListener
        = object : EmptyVisyncNearbyConnectionsListener() {
        override fun onBroadcastingStateChanged(newState: BroadcastingState) {
            if (!_canCallbackChangeConnectionStatus) { return }
            _connectionStatus = newState.mapToGuestConnectionStatus()
        }
        override fun onNewDiscoveredEndpoint(endpoint: DiscoveredEndpoint) {
            val newDiscoveredRoom = object : DiscoveredRoom {
                override val endpointId = endpoint.endpointId
                override val username = endpoint.endpointInfo.endpointName
                override fun connect() {
                    _canCallbackChangeConnectionStatus = false
                    _canAcceptNewConnections = true
                    _connectionStatus = GuestConnectionStatus.CONNECTING
                    nearbyConnections.stopDiscovering()
                    endpoint.initiateConnection(
                        onTaskFailure = { exception ->
                            onRoomJoinFailed(
                                endpoint = endpoint,
                                exception = exception
                            )
                        }
                    )
                }

            }
            _discoveredRooms = _discoveredRooms + newDiscoveredRoom
        }
        override fun onNewConnectionRequest(request: ConnectionRequest) {
            val endpointInfo = EndpointInfo(request)

            if (_canAcceptNewConnections) {
                request.accept(
                    onTaskFailure = { exception ->
                        commonOnAcceptTaskFailure(
                            endpointInfo = endpointInfo,
                            exception = exception,
                            request = request
                        )
                    },
                    onTaskSuccess = {
                        commonOnAcceptTaskSuccess(
                            endpointInfo = endpointInfo
                        )
                    },
                    onConnectionResult = { result ->
                        commonOnAcceptConnectionResult(
                            endpointInfo = endpointInfo,
                            result = result
                        )
                    }
                )
                _canAcceptNewConnections = false
            } else {
                request.reject(
                    onTaskFailure = { exception ->
                        commonOnRejectTaskFailure(endpointInfo, exception, request)
                    },
                    onTaskSuccess = {
                        commonOnRejectTaskSuccess(endpointInfo)
                    },
                )
            }
        }
        override fun onNewRunningConnection(connection: RunningConnection) {
            _connectionStatus = GuestConnectionStatus.CONNECTED
            otherWatchers = listOf(_defaultWatcher.copy(
                endpointId = connection.endpointId,
                username = connection.username
            ))
            messenger.requestOwnEndpointId()
        }
        override fun onRunningConnectionLost(connection: RunningConnection) {
            otherWatchers.find { it.endpointId == connection.endpointId }?.let { watcher ->
                onConnectionLost(connection)
                return
            }

            val warningMsg = (
                "Could not find watcher associated with given connection. " +
                "`Connection lost` callback will not be invoked."
            )
            Log.w(PlaybackSetupHost::class.simpleName, warningMsg)
        }
        override fun onNewMessage(message: String, from: RunningConnection) {
            val fullMessage = messageConverter.decode(message)
            messenger.processMessage(fullMessage, from)
        }
    }

    override fun getPlaybackSetupHostAsWatcher(): Watcher {
        return hostWatcher!!
    }

    override fun startDiscoveringRooms() {
        _discoveredRooms = emptyList()
        nearbyConnections.startDiscovering(
            onTaskFailure = { exception ->
                guestCallbacks.onStartDiscoveringFailed(exception)
            }
        )
    }

    override fun stopDiscoveringRooms() {
        nearbyConnections.stopDiscovering()
    }

    private var _dontReplyToPings = false // TODO: this should be removed once we have visync connection manager
    override fun stopPonging() {
        _dontReplyToPings = true
    }

    override fun setGuestSpecificCallbacks(callbacks: GuestSpecificCallbacks) {
        guestCallbacks = callbacks
    }

    override fun resetToDefaultState() {
        super.resetToDefaultState()
        _guestMutableStateFlow.value = _defaultGuestConnectionState
        _isInConnectionRestoreMode = false
        _canAcceptNewConnections = false
        _canCallbackChangeConnectionStatus = true
        _dontReplyToPings = false
    }

    override fun notifySelfChange() {
        messenger.sendSelfChange()
    }

    override fun setSelectedVideofiles(videofiles: List<Videofile>) {
        val oldSelection = localSelectedFiles
        localSelectedFiles = videofiles
        if (oldSelection != localSelectedFiles) {
            updateMissingVideofiles()
        }
    }

    private fun updateMissingVideofiles() {
        val hostSelectedFilenames = playbackOptions.videofilesMetadata
            .map { it.filename }
        val mySelectedFilenames = localSelectedFiles
            .map { it.metadata.filename }
        val mySelectedAltFilenames = localSelectedFiles
            .map { it.metadata.altFilename }
        val missingFilenames = hostSelectedFilenames
            .filter { it !in mySelectedFilenames && it !in mySelectedAltFilenames }

        selfWatcher = selfWatcher.copy(
            missingVideofileNames = missingFilenames
        )
        messenger.sendMissingFilenames(missingFilenames)
    }

    private val messenger = object : PlaybackSetupMessenger() {
        override fun processMessage(fullMessage: VisyncMessage, sender: RunningConnection) {
            when(fullMessage) {
                is YourOwnEndpointIdMessage -> {
                    selfWatcher = selfWatcher.copy(endpointId = fullMessage.endpointId)
                }
                is RequestOwnEndpointIdMessage -> {
                    if (_isInConnectionRestoreMode) {
                        requestConnectionRestore()
                        _isInConnectionRestoreMode = false
                        return
                    }
                    replyWithTheirEndpointId()
                }
                is RequestMessengerVersionMessage -> {
                    replyWithMyMessengerVersion()
                }
                is RequestPhysicalDeviceMessage -> {
                    replyWithMyPhysicalDevice()
                }
                is AllWatchersUpdateMessage -> {
                    if (_latestWatchersUpdateTimestamp > fullMessage.timestamp) {
                        return
                    }
                    val (newSelf, newOthers) = fullMessage.allWatchers
                        .partition { it.endpointId == selfWatcher.endpointId }
                    selfWatcher = newSelf[0]
                    otherWatchers = newOthers

                    _latestWatchersUpdateTimestamp = fullMessage.timestamp
                }
                is PlaybackOptionsUpdateMessage -> {
                    val oldFilesMetadata = playbackOptions.videofilesMetadata
                    playbackOptions = fullMessage.playbackOptions
                    if (oldFilesMetadata != playbackOptions.videofilesMetadata) {
                        updateMissingVideofiles()
                    }
                }
                is DevicePositionsMessage -> {
                    positionsEditor = fullMessage.positions
                }
                is ConnectionRestoredMessage -> {
                    TODO()
                }
                is CanNotRestoreYourConnectionMessage -> {
                    sender.disconnect()
                }
                is OpenPlayerMessage -> {
                    guestCallbacks.messages.onOpenPlayerMessage()
                }
                is PingMessage -> {
                    if (_dontReplyToPings) { return }
                    sendPong(fullMessage.pingTimestamp)
                }
                is SyncBallMessage -> {
                    guestCallbacks.onSyncBallMessage(EndpointInfo(sender), fullMessage)
                }
            }
        }
    }
    private inline fun <reified T: VisyncMessage> PlaybackSetupMessenger.encodeAndSend(
        fullMessage: T
    ) {
        val msg = messageConverter.encode(fullMessage)
        val hostConnection = _hostConnection!!
        val hostWatcher = otherWatchers.first()
        hostConnection.sendMessage(
            msg = msg,
            onTaskFailure = { exception ->
                commonOnCommunicationFailure(
                    watcher = hostWatcher,
                    runningConnection = hostConnection,
                    msg = msg,
                    exception = exception
                )
            }
        )
    }
    private fun PlaybackSetupMessenger.sendSelfChange() {
        val fullMessage = SelfWatcherUpdateMessage(selfWatcher)
        encodeAndSend(fullMessage)
    }
    private fun PlaybackSetupMessenger.requestOwnEndpointId() {
        val fullMessage = RequestOwnEndpointIdMessage()
        encodeAndSend(fullMessage)
    }
    private fun PlaybackSetupMessenger.requestConnectionRestore() {
        val fullMessage = RestoreMyConnectionMessage(_oldEndpointId)
        encodeAndSend(fullMessage)
    }
    private fun PlaybackSetupMessenger.replyWithTheirEndpointId() {
        val fullMessage = YourOwnEndpointIdMessage(_hostConnection!!.endpointId)
        encodeAndSend(fullMessage)
    }
    private fun PlaybackSetupMessenger.replyWithMyMessengerVersion() {
        val fullMessage = MessengerVersionMessage(PlaybackSetupMessenger.VERSION)
        encodeAndSend(fullMessage)
    }
    private fun PlaybackSetupMessenger.replyWithMyPhysicalDevice() {
        val fullMessage = PhysicalDeviceMessage(selfWatcher.physicalDevice!!)
        encodeAndSend(fullMessage)
    }
    private fun PlaybackSetupMessenger.sendMissingFilenames(filenames: List<String>) {
        val fullMessage = DoNotHaveVideofilesMessage(filenames)
        encodeAndSend(fullMessage)
    }
    private fun PlaybackSetupMessenger.sendPong(pingTimestamp: Long) {
        val fullMessage = PongMessage(pingTimestamp)
        val msg = messageConverter.encode(fullMessage)
        _hostConnection!!.sendMessage(msg)
    }

    private fun onRoomJoinFailed(
        endpoint: DiscoveredEndpoint,
        exception: Exception
    ) {
        _connectionStatus = GuestConnectionStatus.CONNECTION_ERROR
        guestCallbacks.onJoinRoomFailed(
            endpointInfo = EndpointInfo(endpoint.endpointId, endpoint.endpointInfo.endpointName),
            exception = exception,
            actions = object : JoinRoomFailureActions {
                override fun tryAgain(onRetrySuccess: () -> Unit) {
                    _connectionStatus = GuestConnectionStatus.CONNECTING
                    endpoint.initiateConnection(
                        onTaskFailure = { newException ->
                            onRoomJoinFailed(endpoint, newException)
                        },
                        onTaskSuccess = onRetrySuccess // TODO: move this to accept connection success
                    )
                }
                override fun abort() {
                    nearbyConnections.resetToIdle()
                    _guestMutableStateFlow.value = _defaultGuestConnectionState
                    _canCallbackChangeConnectionStatus = true
                }
            }
        )
    }

    private fun onConnectionLost(connection: RunningConnection) {
        val hostWatcherSnapshot = otherWatchers.find { it.endpointId == connection.endpointId }!!
        val endpointInfo = EndpointInfo(hostWatcherSnapshot)
        val wasHandshakeCompleted = isHandshakeCompleted(hostWatcherSnapshot)
        _connectionStatus = GuestConnectionStatus.DISCONNECTED
        otherWatchers = emptyList()
        guestCallbacks.onConnectionLost(
            endpointInfo = endpointInfo,
            actions = object : GuestConnectionLostActions {
                override fun restoreConnection(onFailure: (Exception) -> Unit) {
                    if (wasHandshakeCompleted) {
                        _isInConnectionRestoreMode = true
                        _oldEndpointId = selfWatcher.endpointId
                    }
                    _canAcceptNewConnections = true
                    nearbyConnections.tryInitiateConnection(
                        endpointId = hostWatcherSnapshot.endpointId,
                        onTaskFailure = { exception ->
                            resetToDefaultState()
                            onFailure(exception)
                        },
                        onTaskSuccess = {
                            _connectionStatus = GuestConnectionStatus.CONNECTING
                        }
                    )
                }
                override fun dropConnectionAndGoIdle() {
                    resetToDefaultState()
                    nearbyConnections.resetToIdle()
                }
            }
        )
    }

    private fun isHandshakeCompleted(hostWatcher: Watcher): Boolean {
        return hostWatcher.physicalDevice != null
    }

    private fun BroadcastingState.mapToGuestConnectionStatus(): GuestConnectionStatus {
        return when (this) {
            BroadcastingState.IDLE -> GuestConnectionStatus.IDLE
            BroadcastingState.DISCOVERING -> GuestConnectionStatus.DISCOVERING
            BroadcastingState.ADVERTISING -> GuestConnectionStatus.IDLE
        }
    }
}

interface RoleState {
    fun enterRole()
    fun exitRole()
}

private class HostState(
    nearbyConnections: VisyncNearbyConnections,
    application: Application,
    private val coroutineScope: CoroutineScope,
): PlaybackSetupHost(nearbyConnections, application), RoleState {
    override fun enterRole() {
        startListeningToVisyncNearbyConnections()
        startPinging(
            coroutineScope = coroutineScope,
            pingInterval = DefaultPingingInterval,
            cycleInterval = DefaultPingCyclingInterval
        )
    }
    override fun exitRole() {
        stopPinging()
        stopListeningToVisyncNearbyConnections()
        resetToDefaultState()
        nearbyConnections.resetToIdle()
    }
}

private class GuestState(
    nearbyConnections: VisyncNearbyConnections
): PlaybackSetupGuest(nearbyConnections), RoleState {
    override fun enterRole() {
        startListeningToVisyncNearbyConnections()
    }
    override fun exitRole() {
        stopListeningToVisyncNearbyConnections()
        resetToDefaultState()
        nearbyConnections.resetToIdle()
    }
}

private class IdleState : RoleState {
    override fun enterRole() { }
    override fun exitRole() { }
}

abstract class PlaybackSetupMessenger {
    abstract fun processMessage(fullMessage: VisyncMessage, sender: RunningConnection)

    companion object {

        /** Messaging version.
         *  Is incremented every time breaking changes are made to messaging flow,
         *  like changes to communication structure
         *  or to the contents of messages themselves. */
        const val VERSION = 1
    }
}

interface PlaybackSetupCallbacks {
    fun connectionAcceptTaskFailure(
        endpointInfo: EndpointInfo,
        exception: Exception,
        actions: ConnectionAcceptTaskFailureActions
    )
    fun connectionAcceptTaskSuccess(
        endpointInfo: EndpointInfo
    )
    fun connectionAcceptResultAccepted(
        endpointInfo: EndpointInfo,
    )
    fun connectionAcceptResultRejected(
        endpointInfo: EndpointInfo,
    )
    fun connectionAcceptResultUnresolvedStatusCode(
        endpointInfo: EndpointInfo,
        result: ConnectionResolution
    )
    fun connectionRejectTaskFailure(
        endpointInfo: EndpointInfo,
        exception: Exception,
        actions: ConnectionRejectTaskFailureActions
    )
    fun connectionRejectTaskSuccess(
        endpointInfo: EndpointInfo
    )
    fun onCommunicationFailure(
        watcher: Watcher,
        exception: Exception,
        actions: CommunicationFailureActions
    )
}

interface HostSpecificCallbacks {
    fun onStartAdvertisingFailed(
        exception: Exception,
    )
    fun onMassCommunicationFailure(
        exception: Exception,
        actions: MassCommunicationFailureActions
    )
    fun onWatcherConnectionLost(
        watcher: Watcher,
    )
    fun onConnectionRestoreRequest(
        restoreEntry: RestoreConnectionEntry,
        actions: RestoreConnectionRequestActions
    )
}

interface GuestSpecificCallbacks {
    val messages: GuestMessageCallbacks
    fun onStartDiscoveringFailed(
        exception: Exception,
    )
    fun onJoinRoomFailed(
        endpointInfo: EndpointInfo,
        exception: Exception,
        actions: JoinRoomFailureActions,
    )
    fun onConnectionLost(
        endpointInfo: EndpointInfo,
        actions: GuestConnectionLostActions,
    )
    fun onSyncBallMessage(
        sender: EndpointInfo,
        message: SyncBallMessage,
    )
}

interface GuestMessageCallbacks {
    fun onOpenPlayerMessage()
}

open class EmptyPlaybackSetupCallbacks : PlaybackSetupCallbacks {
    override fun connectionAcceptTaskFailure(
        endpointInfo: EndpointInfo,
        exception: Exception,
        actions: ConnectionAcceptTaskFailureActions
    ) {}
    override fun connectionAcceptTaskSuccess(
        endpointInfo: EndpointInfo
    ) {}
    override fun connectionAcceptResultAccepted(
        endpointInfo: EndpointInfo
    ) {}
    override fun connectionAcceptResultRejected(
        endpointInfo: EndpointInfo
    ) {}
    override fun connectionAcceptResultUnresolvedStatusCode(
        endpointInfo: EndpointInfo,
        result: ConnectionResolution
    ) {}
    override fun connectionRejectTaskFailure(
        endpointInfo: EndpointInfo,
        exception: Exception,
        actions: ConnectionRejectTaskFailureActions
    ) {}
    override fun connectionRejectTaskSuccess(
        endpointInfo: EndpointInfo
    ) {}
    override fun onCommunicationFailure(
        watcher: Watcher,
        exception: Exception,
        actions: CommunicationFailureActions
    ) {}
}

open class EmptyHostSpecificCallbacks : HostSpecificCallbacks {
    override fun onStartAdvertisingFailed(
        exception: Exception,
    ) {}
    override fun onMassCommunicationFailure(
        exception: Exception,
        actions: MassCommunicationFailureActions
    ) {}
    override fun onWatcherConnectionLost(
        watcher: Watcher
    ) {}
    override fun onConnectionRestoreRequest(
        restoreEntry: RestoreConnectionEntry,
        actions: RestoreConnectionRequestActions
    ) {}
}

open class EmptyGuestSpecificCallbacks : GuestSpecificCallbacks {
    override val messages = EmptyGuestMessageCallbacks()
    override fun onStartDiscoveringFailed(
        exception: Exception,
    ) {}
    override fun onJoinRoomFailed(
        endpointInfo: EndpointInfo,
        exception: Exception,
        actions: JoinRoomFailureActions
    ) {}
    override fun onConnectionLost(
        endpointInfo: EndpointInfo,
        actions: GuestConnectionLostActions
    ) {}
    override fun onSyncBallMessage(
        sender: EndpointInfo,
        message: SyncBallMessage
    ) {}
}

open class EmptyGuestMessageCallbacks : GuestMessageCallbacks {
    override fun onOpenPlayerMessage() {}
}

class EndpointInfo(
    endpointId: String,
    username: String,
) {
    constructor(discoveredEndpoint: DiscoveredEndpoint): this(
        endpointId = discoveredEndpoint.endpointId,
        username = discoveredEndpoint.endpointInfo.endpointName
    )
    constructor(connectionRequest: ConnectionRequest): this(
        endpointId = connectionRequest.endpointId,
        username = connectionRequest.connectionInfo.endpointName
    )
    constructor(runningConnection: RunningConnection): this(
        endpointId = runningConnection.endpointId,
        username = runningConnection.username
    )
    constructor(watcher: Watcher): this(
        endpointId = watcher.endpointId,
        username = watcher.username
    )
}

interface ConnectionAcceptTaskFailureActions {
    fun tryAgain(onRetrySuccess: () -> Unit)
    fun rejectConnection()
}

interface ConnectionRejectTaskFailureActions {
    fun tryAgain(onRetrySuccess: () -> Unit)
}

interface CommunicationFailureActions {
    fun tryAgain(onRetrySuccess: () -> Unit)
    fun dropConnection()
}

interface MassCommunicationFailureActions {
    fun tryAgain(onRetrySuccess: () -> Unit)
    fun dropAllConnections()
}

interface JoinRoomFailureActions {
    fun tryAgain(onRetrySuccess: () -> Unit)
    fun abort()
}

interface RestoreConnectionRequestActions {
    fun restoreConnection()
    fun refuseToRestoreConnection()
}

interface GuestConnectionLostActions {
    fun restoreConnection(onFailure: (Exception) -> Unit)
    fun dropConnectionAndGoIdle()
}

data class RestoreConnectionEntry(
    val watcher: Watcher,
    val deviceOnEditor: DeviceOnEditor?
)

/** Searches in the list for provided [value] by reference
 *  and replaces it with [newValue] if reference was found.
 *  @return new list instance with [newValue] if [value] was found,
 *  otherwise returns this list unmodified.*/

fun <T> List<T>.withReplacedValueFoundByReference(
    value: T,
    newValue: T
): List<T> {
    var valueIndex = -1
    for ((index, valueAtIndex) in this.withIndex()) {
        if (valueAtIndex === value) {
            valueIndex = index
            break
        }
    }
    if (valueIndex == -1) {
        return this
    }
    return this.toMutableList().apply {
        this[valueIndex] = newValue
    }
}

fun getCurrentTimestamp(): Long = System.currentTimeMillis()

data class PlaybackSetupConfiguration(
    val username: String,
    val physicalDevice: VisyncPhysicalDevice,
)

@Serializable
data class PingEntry(
    val requestTimestamp: Long,
    val responseTimestamp: Long?,
) {
    val delta = responseTimestamp?.let { it - requestTimestamp }

    companion object {
        val ZeroPingEntry = PingEntry(0L, 0L)
    }
}

@Serializable
data class PingData(
    val pingList: List<PingEntry> = DefaultPingList,
    val pingWeights: List<Float> = DefaultPingWeights
) {
    init {
        assert(pingList.count() == pingWeights.count())
    }

    private val dataLength = pingList.count()

    private val pingDeltas = pingList
        .mapIndexed { index, pingEntry ->
            val lastIndex = dataLength - 1
            if (index != lastIndex) {
                /*  if we don't have a response to ping that is not our latest ping attempt,
                    it is fair to put current time as response time */
                pingEntry.delta ?: (getCurrentTimestamp() - pingEntry.requestTimestamp)
            } else {
                /*  And if it is the last ping then we output 0
                    so this entry is not included in final calculations */
                pingEntry.delta ?: 0L
            }
        }

    private val weightedSum = pingDeltas
        .zip(pingWeights) { ping, weight ->
            ping * weight
        }.sum()

    private val weightsSum = pingWeights
        .zip(pingDeltas) { weight, ping ->
            // By doing min(ping, 1L) we do not include 0 ping values in our final average
            weight * min(ping, 1L)
        }.sum()

    val weightedAverage = weightedSum / weightsSum

    fun withAddedEntry(ping: PingEntry): PingData {
        return copy(
            pingList = pingList.subList(1, dataLength) + listOf(ping)
        )
    }
    fun withReplacedEntry(entry: PingEntry, newEntry: PingEntry): PingData {
        return copy(
            pingList = pingList.withReplacedValueFoundByReference(
                value = entry,
                newValue = newEntry
            )
        )
    }

    companion object {
        val DefaultPingList = listOf(1, 2, 3, 4, 5).map { ZeroPingEntry }
        val DefaultPingWeights = listOf(1f, 2f, 4f, 8f, 16f)
    }
}

@Serializable
data class EndpointPingData(
    val endpointId: String,
    val pingData: PingData
) {
    fun withUnansweredPing(pingTimestamp: Long): EndpointPingData {
        return copy(
            pingData = pingData.withAddedEntry(
                PingEntry(
                    requestTimestamp = pingTimestamp,
                    responseTimestamp = null
                )
            )
        )
    }
}

data class HostConnectionState(
    val isAdvertising: Boolean,
    val allWatcherPings: List<EndpointPingData>
) {
    val hasConnections
        get() = allWatcherPings.isNotEmpty()
}

data class GuestConnectionState(
    val connectionStatus: GuestConnectionStatus,
    val discoveredRooms: List<DiscoveredRoom>,
)

enum class GuestConnectionStatus {
    IDLE, DISCOVERING, CONNECTING, CONNECTED, CONNECTION_ERROR, DISCONNECTED
}

interface DiscoveredRoom {
    val endpointId: String
    val username: String

    fun connect()
}

@Serializable
data class DevicePositionsEditor(
    val videoOnEditor: VideoOnEditor,
    val devicesOnEditor: List<DeviceOnEditor>,
    val cameraView: EditorCameraView,
) {
    fun withAddedWatcher(
        watcher: Watcher,
        color: Long? = null
    ): DevicePositionsEditor {
        val usedColors = devicesOnEditor.map { it.brushColor }
        val nextColor = color ?: DEFAULT_DEVICE_COLORS.minByOrNull { defaultColor ->
            usedColors.count { it == defaultColor } // least used color
        } ?: DEFAULT_DEVICE_COLORS[0]
        val rightmostDevice = devicesOnEditor.maxByOrNull { it.deviceRight }
        val newDevice = DeviceOnEditor(
            watcherInfo = WatcherInfo(watcher),
            brushColor = nextColor,
            mmOffsetX = (rightmostDevice?.deviceRight ?: (DEFAULT_DEVICE_OFFSET_X - DEFAULT_DEVICE_GAP)) + DEFAULT_DEVICE_GAP,
            mmOffsetY = (rightmostDevice?.deviceTop ?: DEFAULT_DEVICE_OFFSET_Y),
            mmDeviceWidth = watcher.physicalDevice!!.mmDeviceWidth,
            mmDeviceHeight = watcher.physicalDevice.mmDeviceHeight,
            mmDisplayWidth = watcher.physicalDevice.mmDisplayWidth,
            mmDisplayHeight = watcher.physicalDevice.mmDisplayHeight,
        )
        return copy(
            devicesOnEditor = devicesOnEditor + newDevice
        )
    }

    fun withRemovedWatcher(watcher: Watcher): DevicePositionsEditor {
        return copy(
            devicesOnEditor = devicesOnEditor
                .filter { it.watcherInfo.endpointId != watcher.endpointId }
        )
    }

    fun withReplacedVideo(videofile: Videofile): DevicePositionsEditor {
        val newVideoOnEditor = VideoOnEditor(
            videoUri = videofile.uri,
            videoMetadata = videofile.metadata,
            mmOffsetX = videoOnEditor.mmOffsetX,
            mmOffsetY = videoOnEditor.mmOffsetY,
            mmWidth = videofile.metadata.width / 10,
            mmHeight = videofile.metadata.height / 10,
        )
        return copy(
            videoOnEditor = newVideoOnEditor
        )
    }
    companion object {
        fun create(
            watchers: List<Watcher>,
            videofile: Videofile,
            deviceColors: List<Long> = DEFAULT_DEVICE_COLORS,
        ): DevicePositionsEditor {
            val initialOffsetX = DEFAULT_DEVICE_OFFSET_X
            val initialOffsetY = DEFAULT_DEVICE_OFFSET_Y
            val videoOnEditor = VideoOnEditor(
                videoUri = videofile.uri,
                videoMetadata = videofile.metadata,
                mmOffsetX = initialOffsetX * 2,
                mmOffsetY = initialOffsetY * 2,
                mmWidth = videofile.metadata.width / 10,
                mmHeight = videofile.metadata.height / 10,
            )
            val devicesOnEditor = mutableListOf<DeviceOnEditor>()
            var prevDevice = DeviceOnEditor.ZeroDimensionsDevice.copy(
                mmOffsetX = initialOffsetX - DEFAULT_DEVICE_GAP,
                mmOffsetY = initialOffsetY,
            )
            val colorsCount = deviceColors.count()
            var nextColorIndex = 0
            val nextColor: () -> Long = {
                deviceColors[nextColorIndex].also {
                    nextColorIndex = (nextColorIndex + 1) % colorsCount
                }
            }
            for (watcher in watchers) {
                val newDevice = DeviceOnEditor(
                    watcherInfo = WatcherInfo(watcher),
                    brushColor = nextColor(),
                    mmOffsetX = prevDevice.deviceRight + DEFAULT_DEVICE_GAP,
                    mmOffsetY = prevDevice.deviceTop,
                    mmDeviceWidth = watcher.physicalDevice!!.mmDeviceWidth,
                    mmDeviceHeight = watcher.physicalDevice.mmDeviceHeight,
                    mmDisplayWidth = watcher.physicalDevice.mmDisplayWidth,
                    mmDisplayHeight = watcher.physicalDevice.mmDisplayHeight,
                )
                devicesOnEditor += newDevice
                prevDevice = newDevice
            }
            val cameraView = EditorCameraView(
                mmViewOffsetX = 0f,
                mmViewOffsetY = 0f,
                zoom = 1f
            )
            return DevicePositionsEditor(
                videoOnEditor = videoOnEditor,
                devicesOnEditor = devicesOnEditor,
                cameraView = cameraView,
            )
        }

        private const val DEFAULT_DEVICE_OFFSET_X = 10f
        private const val DEFAULT_DEVICE_OFFSET_Y = 10f
        private const val DEFAULT_DEVICE_GAP = 10f
        private val DEFAULT_DEVICE_COLORS = listOf(
            0xFF4cb4f0,
            0xFFff9a00,
            0xFF0b9f57,
            0xFFba338a,
        )
    }
}

@Serializable
data class WatcherInfo(
    val endpointId: String,
    val username: String,
) {
    constructor(watcher: Watcher): this(watcher.endpointId, watcher.username)
}

@Serializable
data class DeviceOnEditor(
    val watcherInfo: WatcherInfo,
    val brushColor: Long,
    val mmOffsetX: Float,
    val mmOffsetY: Float,
    val mmDeviceWidth: Float,
    val mmDeviceHeight: Float,
    val mmDisplayWidth: Float,
    val mmDisplayHeight: Float,
    /** Vertical bias of 0 means display is perfectly centered */
    val displayVerticalBias: Float = -0.4f,
) {
    @Transient private val horizontalGap = mmDeviceWidth - mmDisplayWidth
    @Transient private val verticalGap = mmDeviceHeight - mmDisplayHeight

    @Transient private val displayRelativeOffset = Offset(
        x = horizontalGap / 2,
        y = (verticalGap / 2) * (1 + displayVerticalBias)
    )

    @Transient val deviceLeft = mmOffsetX
    @Transient val deviceRight = deviceLeft + mmDeviceWidth
    @Transient val deviceTop = mmOffsetY
    @Transient val deviceBottom  = deviceTop + mmDeviceHeight

    @Transient val displayLeft = deviceLeft + displayRelativeOffset.x
    @Transient val displayRight = displayLeft + mmDisplayWidth
    @Transient val displayTop = deviceTop + displayRelativeOffset.y
    @Transient val displayBottom = displayTop + mmDisplayHeight

    companion object {
        val ZeroDimensionsDevice = DeviceOnEditor(
            watcherInfo = WatcherInfo("", ""),
            brushColor = 0L,
            mmOffsetX = 0f,
            mmOffsetY = 0f,
            mmDeviceWidth = 0f,
            mmDeviceHeight = 0f,
            mmDisplayWidth = 0f,
            mmDisplayHeight = 0f,
        )
    }
}

@Serializable
data class EditorCameraView(
    val mmViewOffsetX: Float,
    val mmViewOffsetY: Float,
    val zoom: Float
) {
    fun withAddedOffset(x: Float, y: Float): EditorCameraView {
        return copy(
            mmViewOffsetX = mmViewOffsetX + x,
            mmViewOffsetY = mmViewOffsetY + y
        )
    }

    fun zoomedBy(zoomMultiplier: Float, mmPivotPoint: Offset): EditorCameraView {
        return zoomedTo(
            targetZoom = zoom * zoomMultiplier,
            mmPivotPoint = mmPivotPoint
        )
    }

    fun zoomedTo(targetZoom: Float, mmPivotPoint: Offset): EditorCameraView {
        val newZoom = targetZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val zoomMultiplier = newZoom / zoom
        val cameraTopLeft = Offset(x = mmViewOffsetX, y = mmViewOffsetY)
        val offsetChange = (mmPivotPoint - cameraTopLeft) * ((zoomMultiplier - 1) / zoomMultiplier)
        return copy(
            mmViewOffsetX = mmViewOffsetX + offsetChange.x,
            mmViewOffsetY = mmViewOffsetY + offsetChange.y,
            zoom = zoom * zoomMultiplier
        )
    }

    companion object {
        const val MAX_ZOOM = 2f
        const val MIN_ZOOM = 0.5f
    }
}

@Serializable
data class VideoOnEditor private constructor(
    val videoMetadata: VideoMetadata,
    val mmOffsetX: Float,
    val mmOffsetY: Float,
    val mmWidth: Float,
    val mmHeight: Float,
    val originalWidth: Float = mmWidth,
    val originalHeight: Float = mmHeight,
) {
    @Transient var uri: Uri = Uri.EMPTY
        private set
    @Transient val mmLeft = mmOffsetX
    @Transient val mmRight = mmLeft + mmWidth
    @Transient val mmTop = mmOffsetY
    @Transient val mmBottom = mmTop + mmHeight
    @Transient val mmCenter = Offset(x = mmOffsetX + mmWidth / 2, y = mmOffsetY + mmHeight / 2)

    constructor(
        videoUri: Uri,
        videoMetadata: VideoMetadata,
        mmOffsetX: Float,
        mmOffsetY: Float,
        mmWidth: Float,
        mmHeight: Float,
        originalWidth: Float = mmWidth,
        originalHeight: Float = mmHeight,
    ) : this(
        videoMetadata = videoMetadata,
        mmOffsetX = mmOffsetX,
        mmOffsetY = mmOffsetY,
        mmWidth = mmWidth,
        mmHeight = mmHeight,
        originalWidth = originalWidth,
        originalHeight = originalHeight,
    ) {
        this.uri = videoUri
    }

    suspend fun createThumbnail(context: Context): ImageBitmap = coroutineScope {
        val getPlaceholderImage: () -> ImageBitmap = {
            val placeholderImageId = R.drawable.doxie_picture
            ImageBitmap.imageResource(context.resources, placeholderImageId)
        }
        if (uri == Uri.EMPTY) {
            return@coroutineScope getPlaceholderImage()
        }
        val metadataRetriever = MediaMetadataRetriever()
        metadataRetriever.setDataSource(context, uri)

        // "A Bitmap containing a representative video frame" so it should be fine
        // but in reality it can return really bad frames like full black image when a clearly better choice exists
        val defaultThumbnail = metadataRetriever.frameAtTime

        // get additional frames that could be better
        val middleFrame = metadataRetriever.getFrameAtTime(videoMetadata.duration * 1000 / 2)

        val superDarkThreshold = 0.01f

        if (defaultThumbnail != null && middleFrame != null) {
            Log.d("Luminance", "started calculations...")
            val defImageLuminanceJob = async { defaultThumbnail.calculateLuminance() }
            val middleImageLuminanceJob = async { middleFrame.calculateLuminance() }
            val defImageLuminance = defImageLuminanceJob.await()
            val middleImageLuminance = middleImageLuminanceJob.await()
            Log.d("Luminance", "defImageLuminance: $defImageLuminance")
            Log.d("Luminance", "middleImageLuminance: $middleImageLuminance")
            if (defImageLuminance < superDarkThreshold && middleImageLuminance > defImageLuminance) {
                return@coroutineScope middleFrame.asImageBitmap()
            }
        }
        return@coroutineScope defaultThumbnail?.asImageBitmap() ?: getPlaceholderImage()
    }

    // seems like use of coroutines on Dispatchers.Default doesn't save any computation time,
    // at least not on ZB631KL when processing FHD video
    private suspend fun Bitmap.calculateLuminanceWithCoroutines(): Float = coroutineScope {
        val jobCount = 8
        val pixelCount = width * height
        val pixelsPerJob = pixelCount / jobCount
        val allPixels = IntArray(size = pixelCount)
        getPixels(allPixels, 0, width, 0, 0, width, height)
        val calculatePartialLuminance: (Int, Int) -> Double = { fromPixel, toPixel ->
            var localSum = 0.0
            for (i in fromPixel ..< toPixel) {
                val px = allPixels[i]
                val r = VisyncColor.SRGB_TO_LINEAR_RGB_LOOKUP_TABLE[(px shr 16) and 255]
                val g = VisyncColor.SRGB_TO_LINEAR_RGB_LOOKUP_TABLE[(px shr 8) and 255]
                val b = VisyncColor.SRGB_TO_LINEAR_RGB_LOOKUP_TABLE[(px) and 255]
                localSum += 0.2126 * r + 0.7152 * g + 0.0722 * b
            }
            localSum
        }
        val jobResults = (0 ..< jobCount).map { jobIndex ->
            val isLastJob = jobIndex == jobCount - 1
            val startPx = pixelsPerJob * jobIndex
            if (isLastJob) {
                return@map async(Dispatchers.Default) {
                    return@async calculatePartialLuminance(startPx, pixelCount)
                }
            }
            return@map async(Dispatchers.Default) {
                val endPx = startPx + pixelsPerJob
                return@async calculatePartialLuminance(startPx, endPx)
            }
        }

        val luminanceSum = jobResults.sumOf { it.await() }
        return@coroutineScope (luminanceSum / pixelCount).toFloat()
    }

    private fun Bitmap.calculateLuminance(): Float {
        var luminanceSum = 0.0
        val allPixels = IntArray(size = width * height)
        // 2. using getPixels instead of getPixel(i, j)
        //    reduces computation time from ~1s to ~0.2s (device: ZB631KL)
        getPixels(allPixels, 0, width, 0, 0, width, height)
        for (px in allPixels) {
            // 1. calculating luminance manually instead of
            //    constructing Color(px) and calling .luminance
            //    reduces computation time from ~3s to ~1s (device: ZB631KL)
            val r = VisyncColor.SRGB_TO_LINEAR_RGB_LOOKUP_TABLE[(px shr 16) and 255]
            val g = VisyncColor.SRGB_TO_LINEAR_RGB_LOOKUP_TABLE[(px shr 8) and 255]
            val b = VisyncColor.SRGB_TO_LINEAR_RGB_LOOKUP_TABLE[(px) and 255]
            luminanceSum += 0.2126 * r + 0.7152 * g + 0.0722 * b
        }
        return (luminanceSum / (width * height)).toFloat()
    }

    fun withAddedOffset(x: Float, y: Float): VideoOnEditor {
        return copy(
            mmOffsetX = mmOffsetX + x,
            mmOffsetY = mmOffsetY + y
        )
    }

    fun zoomedBy(zoomMultiplier: Float, mmPivotPoint: Offset): VideoOnEditor {
        if (zoomMultiplier == 1f) return this
        val videoTopLeft = Offset(x = mmOffsetX, y = mmOffsetY)
        val offsetChange = (mmPivotPoint - videoTopLeft) * ((zoomMultiplier - 1) / zoomMultiplier)
        val newWidth = mmWidth * zoomMultiplier
        val newHeight = mmHeight * zoomMultiplier
        if (newWidth < 50f || newWidth > 500f) { return this }
        val originalAspectRatio = originalWidth / originalHeight
        val newAspectRatio = newWidth / newHeight
        val aspectRatioDiff = (originalAspectRatio - newAspectRatio).absoluteValue
        val allowedDeformation = 0.01f
        if (aspectRatioDiff / originalAspectRatio > allowedDeformation) {
            return copy(
                mmOffsetX = mmOffsetX - offsetChange.x,
                mmOffsetY = mmOffsetY - offsetChange.y,
                mmWidth = newHeight * originalAspectRatio,
                mmHeight = newHeight,
            )
        }
        return copy(
            mmOffsetX = mmOffsetX - offsetChange.x,
            mmOffsetY = mmOffsetY - offsetChange.y,
            mmWidth = newWidth,
            mmHeight = newHeight,
        )
    }
}

fun VideoOnEditor.containsPoint(point: Offset): Boolean {
    return  point.x > mmLeft &&
            point.x < mmRight &&
            point.y > mmTop &&
            point.y < mmBottom
}

fun DeviceOnEditor.containsPoint(point: Offset): Boolean {
    return  point.x > deviceLeft &&
            point.x < deviceRight &&
            point.y > deviceTop &&
            point.y < deviceBottom
}

fun DeviceOnEditor.isIntersecting(other: DeviceOnEditor): Boolean {
    return  deviceLeft <= other.deviceRight &&
            deviceRight >= other.deviceLeft &&
            deviceTop <= other.deviceBottom &&
            deviceBottom >= other.deviceTop
}

fun DeviceOnEditor.isIntersectingAny(others: List<DeviceOnEditor>): Boolean {
    return others.any { this.isIntersecting(it) }
}