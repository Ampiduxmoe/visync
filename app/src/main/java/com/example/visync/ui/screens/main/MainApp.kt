package com.example.visync.ui.screens.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.visync.data.playlists.PlaylistWithVideofiles
import com.example.visync.ui.components.navigation.Route
import com.example.visync.ui.components.navigation.MainAppNavigation
import com.example.visync.ui.components.navigation.NavigationType
import com.example.visync.ui.screens.main.playlists.PlaylistsScreen
import com.example.visync.ui.screens.main.playlists.PlaylistsScreenViewModel
import com.example.visync.ui.screens.main.rooms.RoomsScreen
import com.example.visync.ui.screens.main.rooms.RoomsScreenViewModel
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Composable
fun MainApp(
    windowSize: WindowSizeClass,
    mainAppUiState: MainAppUiState,
    mainAppNavigationUiState: MainAppNavigationUiState,
    playPlaylist: (PlaybackStartOptions) -> Unit,
) {
    val navigationType: NavigationType
    val preferredDisplayMode: ContentDisplayMode

    Log.i("WindowSize", "widthSizeClass=${windowSize.widthSizeClass}")
    Log.i("WindowSize", "heightSizeClass=${windowSize.heightSizeClass}")

    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            navigationType = NavigationType.BOTTOM_NAVBAR_AND_DRAWER
            preferredDisplayMode = ContentDisplayMode.SINGLE_COLUMN
        }
        WindowWidthSizeClass.Medium -> {
            navigationType = NavigationType.RAIL_AND_DRAWER
            preferredDisplayMode = ContentDisplayMode.SINGLE_COLUMN
        }
        WindowWidthSizeClass.Expanded -> {
            navigationType = NavigationType.CUSTOM_PERMANENT_DRAWER
            preferredDisplayMode = ContentDisplayMode.DUAL_COLUMN
        }
        else -> {
            navigationType = NavigationType.BOTTOM_NAVBAR_AND_DRAWER
            preferredDisplayMode = ContentDisplayMode.SINGLE_COLUMN
        }
    }

    val navController = rememberNavController()
    MainAppNavigation(
        uiState = mainAppNavigationUiState,
        navigationType = navigationType,
        navController = navController
    ) {
        NavHost(
            navController = navController,
            startDestination = Route.Playlists.routeString,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Route.Playlists.routeString) {
                val playlistsScreenViewModel = hiltViewModel<PlaylistsScreenViewModel>()
                val playlistsUiState by playlistsScreenViewModel
                    .uiState.collectAsStateWithLifecycle()
                PlaylistsScreen(
                    preferredDisplayMode = preferredDisplayMode,
                    playlistsUiState = playlistsUiState,
                    openPlaylist = { playlistsScreenViewModel.setSelectedPlaylist(it.id) },
                    closePlaylist = playlistsScreenViewModel::closeDetailScreen,
                    addVideoToPlaylistFromUri = playlistsScreenViewModel::addVideoToPlaylistFromUri,
                    playVideofile = { videofile ->
                        val parentPlaylistWithVideofiles = playlistsUiState.playlists
                            .find { playlistWithVideofiles ->
                                playlistWithVideofiles.playlist.id == videofile.playlistId
                            }!! // since videofile always belongs to a playlist
                        val videofileIndex = parentPlaylistWithVideofiles.videofiles
                            .indexOf(videofile)
                        val playbackStartOptions = PlaybackStartOptions(
                            playlist = parentPlaylistWithVideofiles,
                            startFrom = videofileIndex,
                            playbackMode = VisyncPlaybackMode.GROUP
                        )
                        playPlaylist(playbackStartOptions)
                    },
                    openDrawer = openDrawer
                )
            }
            composable(Route.RoomsJoin.routeString) {
                val roomsScreenViewModel = hiltViewModel<RoomsScreenViewModel>()
                val roomsUiState by roomsScreenViewModel
                    .uiState.collectAsStateWithLifecycle()
                RoomsScreen(
                    roomsUiState = roomsUiState
                )
            }
            composable(Route.MyProfile.routeString) {
                Column {

                }
            }
            composable(Route.Friends.routeString) {
                Column {

                }
            }
            composable(Route.RoomsManage.routeString) {
                Column {

                }
            }
            composable(Route.AppSettings.routeString) {
                @SuppressLint("InlinedApi")
                val requiredPermissions = pickRequiredPermissions(
                    sdkVersion = Build.VERSION.SDK_INT,
                    permissionsWithVersions = listOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION  to  1..999,
                        Manifest.permission.ACCESS_FINE_LOCATION    to 29..32,
                        Manifest.permission.BLUETOOTH_ADVERTISE     to 31..999,
                        Manifest.permission.BLUETOOTH_CONNECT       to 31..999,
                        Manifest.permission.BLUETOOTH_SCAN          to 31..999,
                        Manifest.permission.NEARBY_WIFI_DEVICES     to 33..999,
                    )
                )
                val context = LocalContext.current
                val permissionsState = remember {
                    mutableStateOf(requiredPermissions.associateWith {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    })
                }
                val askForPermissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { grantStatuses ->
                        permissionsState.value = permissionsState.value + grantStatuses
                    }
                )
                Column {
                    Text(
                        text = permissionsState.value
                            .map { entry ->
                                "${entry.key} ${if(entry.value) "granted" else "denied"}"
                            }.joinToString("\n")
                    )
                    IconButton(
                        onClick = {
                            if (permissionsState.value.values.all { it }) {
                                navController.navigate("testNearbyConnections") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                val remainingPermissions = permissionsState.value
                                    .filter { !it.value }
                                    .map { it.key }
                                Log.d("permissions",
                                    "trying to ask for these permissions:\n${
                                        remainingPermissions.joinToString(separator="\n")}"
                                )
                                askForPermissionsLauncher.launch(remainingPermissions.toTypedArray())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "ask for required permissions"
                        )
                    }
                }
            }
            composable("testNearbyConnections") {
                val nearbyConnectionsViewModel = hiltViewModel<NearbyConnectionsViewModel>()
                val nearbyConnectionsState by nearbyConnectionsViewModel
                    .connectionsState.collectAsStateWithLifecycle()
                val context = LocalContext.current
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("current status = ${nearbyConnectionsState.status}")
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row {
                            IconButton(
                                onClick = { nearbyConnectionsViewModel.startAdvertising(context) }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "start advertising"
                                )
                            }
                            Text("start advertising")
                        }
                        Row {
                            IconButton(
                                onClick = { nearbyConnectionsViewModel.stopAdvertising?.invoke() }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "stop advertising"
                                )
                            }
                            Text("stop advertising")
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row {
                            IconButton(
                                onClick = { nearbyConnectionsViewModel.startDiscovering(context) }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "start discovering"
                                )
                            }
                            Text("start discovering")
                        }
                        Row {
                            IconButton(
                                onClick = { nearbyConnectionsViewModel.stopDiscovering?.invoke() }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "stop discovering"
                                )
                            }
                            Text("stop discovering")
                        }
                    }
                    val currentMessage = remember { mutableStateOf("") }
                    TextField(
                        value = currentMessage.value,
                        onValueChange = {
                            currentMessage.value = it
                        }
                    )
                    Button(
                        onClick = {
                            nearbyConnectionsViewModel.sendMessage(currentMessage.value)
                            currentMessage.value = ""
                        }
                    ) {
                        Text("send this message!")
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp)
                    ) {
                        items(nearbyConnectionsState.messages) {msg ->
                            Text(msg)
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier
                            .padding(4.dp)
                            .weight(1f)) {
                            Text("discovered points:")
                            for (endpoint in nearbyConnectionsState.discoveredEndpoints) {
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Text(endpoint.endpointId)
                                    Text(endpoint.endpointInfo.endpointName)
                                    Text(
                                        text = "connect",
                                        modifier = Modifier.clickable { endpoint.initiateConnection() }
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier
                            .padding(4.dp)
                            .weight(1f)) {
                            Text("connection requests:")
                            for (request in nearbyConnectionsState.connectionRequests) {
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Text("${request.endpointId}:")
                                    Row {
                                        Text(
                                            text = "  +  ",
                                            modifier = Modifier.clickable { request.accept() })
                                        Text(
                                            text = "  -  ",
                                            modifier = Modifier.clickable { request.reject() })
                                    }
                                }
                            }
                        }
                        Column(modifier = Modifier
                            .padding(4.dp)
                            .weight(1f)) {
                            Text("running connections:")
                            for (endpoint in nearbyConnectionsState.runningConnections) {
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Text(endpoint.endpointId)
                                    Text(
                                        text = "disconnect",
                                        modifier = Modifier.clickable { endpoint.disconnect() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class ContentDisplayMode {
    SINGLE_COLUMN, DUAL_COLUMN
}

@HiltViewModel
class NearbyConnectionsViewModel @Inject constructor(

) : ViewModel() {

    private val _connectionsState = MutableStateFlow(
        NearbyConnectionsState(
            status = "idle",
            discoveredEndpoints = listOf(),
            connectionRequests = listOf(),
            runningConnections = listOf(),
            messages = listOf()
        )
    )
    val connectionsState: StateFlow<NearbyConnectionsState> = _connectionsState

    init {

    }

    var sendMessage: (msg: String) -> Unit = {}
        private set

    var stopAdvertising: (() -> Unit)? = null
        private set

    fun startAdvertising(context: Context) {
        if (stopAdvertising != null || stopDiscovering != null) {
            return
        }
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
        val connectionsClient = Nearby.getConnectionsClient(context)
        connectionsClient
            .startAdvertising(
                "advertiser",
                "mySuperApp",
                getDefaultConnectionLifecycleCallback(connectionsClient),
                advertisingOptions
            )
            .addOnSuccessListener {
                Log.d("startAdvertising", "started advertising")
                _connectionsState.value = _connectionsState.value.copy(
                    status = "advertising"
                )
            }
            .addOnFailureListener {
                Log.e("startAdvertising", "error!", it)
            }
        stopAdvertising = {
            connectionsClient.stopAdvertising()
            stopAdvertising = null
            _connectionsState.value = _connectionsState.value.copy(
                status = "idle"
            )
        }
    }

    var stopDiscovering: (() -> Unit)? = null
        private set

    fun startDiscovering(context: Context) {
        if (stopAdvertising != null || stopDiscovering != null) {
            return
        }
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
        val connectionsClient = Nearby.getConnectionsClient(context)
        connectionsClient
            .startDiscovery(
                "mySuperApp",
                object : EndpointDiscoveryCallback() {
                    override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                        Log.d("startDiscovering", "onEndpointFound: $endpointId (${info.endpointName})")
                        addDiscoveredEndpoint(
                            discoveredEndpointId = endpointId,
                            endpointInfo = info,
                            connectionsClient = connectionsClient
                        )
                    }
                    override fun onEndpointLost(endpointId: String) {
                        Log.d("startDiscovering", "onEndpointLost: $endpointId")
                        removeDiscoveredEndpoint(endpointId)
                    }
                },
                discoveryOptions
            )
            .addOnSuccessListener {
                Log.d("startAdvertising", "started discovering")
                _connectionsState.value = _connectionsState.value.copy(
                    status = "discovering"
                )
            }
            .addOnFailureListener {
                Log.e("startDiscovering", "error!", it)
            }
        stopDiscovering = {
            connectionsClient.stopDiscovery()
            stopDiscovering = null
            _connectionsState.value = _connectionsState.value.copy(
                status = "idle"
            )
        }
    }

    private fun addDiscoveredEndpoint(
        discoveredEndpointId: String,
        endpointInfo: DiscoveredEndpointInfo,
        connectionsClient: ConnectionsClient,
    ) {
        val discoveredEndpoint = DiscoveredEndpoint(
            endpointId = discoveredEndpointId,
            endpointInfo = endpointInfo,
            connectionLifecycleCallback = getDefaultConnectionLifecycleCallback(connectionsClient),
            onConnectionRequestSent = {
                Log.d("Connection", "successfully sent connection request")
                removeDiscoveredEndpoint(discoveredEndpointId)
            },
            onConnectionRequestFailure = {
                Log.e("Connection", "failed to send connection request", it)
                removeDiscoveredEndpoint(discoveredEndpointId)
            },
            connectionsClient = connectionsClient
        )
        _connectionsState.value = _connectionsState.value.copy(
            discoveredEndpoints = _connectionsState.value.discoveredEndpoints + discoveredEndpoint
        )
    }

    private fun removeDiscoveredEndpoint(endpointId: String) {
        _connectionsState.value = _connectionsState.value.copy(
            discoveredEndpoints = _connectionsState.value.discoveredEndpoints
                .filter { it.endpointId != endpointId }
        )
    }

    private fun addConnectionRequest(
        endpointId: String,
        connectionInfo: ConnectionInfo,
        connectionsClient: ConnectionsClient,
    ) {
        Log.d("addConnectionRequest", "adding connection request")
        Log.d("addConnectionRequest", "endpointId is $endpointId")
        Log.d("addConnectionRequest", "auth digits are ${connectionInfo.authenticationDigits}")
        val connectionRequest = ConnectionRequest(
            endpointId = endpointId,
            connectionInfo = connectionInfo,
            payloadCallback = object : PayloadCallback() {
                override fun onPayloadReceived(endpointId: String, payload: Payload) {
                    Log.d("NearbyConnections", "payload received from endpoint $endpointId")
                    if (payload.type == Payload.Type.BYTES) {
                        payload.asBytes()?.let { receivedBytes ->
                            val message = String(receivedBytes, Charsets.UTF_8)
                            addMessage("$endpointId: $message")
                        }
                    }
                }
                override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
                    Log.d("NearbyConnections", "onPayloadTransferUpdate")
                }
            },
            connectionsClient = connectionsClient
        )
        _connectionsState.value = _connectionsState.value.copy(
            connectionRequests = _connectionsState.value.connectionRequests + connectionRequest
        )
        Log.d("addConnectionRequest", "added connection request")
    }

    private fun removeConnectionRequest(endpointId: String) {
        _connectionsState.value = _connectionsState.value.copy(
            connectionRequests = _connectionsState.value.connectionRequests
                .filter { it.endpointId != endpointId }
        )
    }

    private fun getDefaultConnectionLifecycleCallback(
        connectionsClient: ConnectionsClient
    ) = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d("ConnectionLifecycleCallback", "onConnectionInitiated ($endpointId)")
            addConnectionRequest(
                endpointId = endpointId,
                connectionInfo = info,
                connectionsClient = connectionsClient
            )
        }
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d("ConnectionLifecycleCallback", "connected to $endpointId!")
                    removeConnectionRequest(endpointId)
                    addRunningConnection(
                        endpointId = endpointId,
                        connectionsClient = connectionsClient
                    )
                    sendMessage = { msg ->
                        val payload = Payload.fromBytes(msg.toByteArray(Charsets.UTF_8))
                        connectionsClient
                            .sendPayload(endpointId, payload)
                            .addOnSuccessListener {
                                Log.d("ConnectionLifecycleCallback", "successfully sent a message")
                                addMessage("you: $msg")
                            }
                            .addOnFailureListener {
                                Log.e("ConnectionLifecycleCallback", "could not send a message", it)
                            }
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d("ConnectionLifecycleCallback", "connection to $endpointId rejected")
                    removeConnectionRequest(endpointId)
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.d("ConnectionLifecycleCallback", "connection status error")
                }
                else -> {
                    Log.d("ConnectionLifecycleCallback", "unknown status code")
                }
            }
        }
        override fun onDisconnected(endpointId: String) {
            Log.d("ConnectionLifecycleCallback", "onDisconnected from $endpointId!")
            removeRunningConnection(endpointId)
            sendMessage = {}
        }
    }

    private fun addRunningConnection(
        endpointId: String,
        connectionsClient: ConnectionsClient
    ) {
        val runningConnection = RunningConnection(
            endpointId = endpointId,
            connectionsClient = connectionsClient
        )
        _connectionsState.value = _connectionsState.value.copy(
            runningConnections = _connectionsState.value.runningConnections + runningConnection
        )
    }

    private fun removeRunningConnection(endpointId: String) {
        _connectionsState.value = _connectionsState.value.copy(
            runningConnections = _connectionsState.value.runningConnections
                .filter { it.endpointId != endpointId }
        )
    }

    private fun addMessage(msg: String) {
        _connectionsState.value = _connectionsState.value.copy(
            messages = _connectionsState.value.messages + msg
        )
    }
}

data class NearbyConnectionsState(
    val status: String,
    val discoveredEndpoints: List<DiscoveredEndpoint>,
    val connectionRequests: List<ConnectionRequest>,
    val runningConnections: List<RunningConnection>,
    val messages: List<String>,
)

class DiscoveredEndpoint(
    val endpointId: String,
    val endpointInfo: DiscoveredEndpointInfo,
    private val connectionLifecycleCallback: ConnectionLifecycleCallback,
    private val onConnectionRequestSent: () -> Unit,
    private val onConnectionRequestFailure: (Exception) -> Unit,
    private val connectionsClient: ConnectionsClient,
) {
    fun initiateConnection() {
        connectionsClient
            .requestConnection(
                "discoverer",
                endpointId,
                connectionLifecycleCallback
            )
            .addOnSuccessListener{
                onConnectionRequestSent()
            }
            .addOnFailureListener {
                onConnectionRequestFailure(it)
            }
    }
}

class ConnectionRequest(
    val endpointId: String,
    val connectionInfo: ConnectionInfo,
    private val payloadCallback: PayloadCallback,
    private val connectionsClient: ConnectionsClient,
) {

    init {
        Log.d("ConnectionRequest", "init")
    }

    fun accept() {
        connectionsClient.acceptConnection(endpointId, payloadCallback)
    }

    fun reject() {
        connectionsClient.rejectConnection(endpointId)
    }
}

class RunningConnection(
    val endpointId: String,
    private val connectionsClient: ConnectionsClient,
) {
    fun disconnect() {
        connectionsClient.disconnectFromEndpoint(endpointId)
    }
}

private fun pickRequiredPermissions(
    sdkVersion: Int,
    permissionsWithVersions: List<Pair<String, IntRange>>
): List<String> {
    val requiredPermissions = mutableListOf<String>()
    for (entry in permissionsWithVersions) {
        val permission = entry.first
        val versionRange = entry.second
        if (sdkVersion in versionRange) {
            requiredPermissions.add(permission)
        }
    }
    return requiredPermissions
}

data class PlaybackStartOptions(
    val playlist: PlaylistWithVideofiles,
    val startFrom: Int,
    val playbackMode: VisyncPlaybackMode,
)

enum class VisyncPlaybackMode {
    ALONE, GROUP
}