package com.example.visync.ui.screens.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Composable
fun MainApp(
    windowSize: WindowSizeClass,
    mainAppUiState: MainAppUiState,
    playPlaylist: (playlist: PlaylistWithVideofiles, startFrom: Int) -> Unit,
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
                        playPlaylist(
                            /* playlist = */ parentPlaylistWithVideofiles,
                            /* startFrom = */ videofileIndex
                        )
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
                val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    listOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                } else {
                    listOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    )
                }
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
                Column {
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
                    Text("discovered points:")
                    Column {
                        for (discoveredPoint in nearbyConnectionsState.discoveredPoints) {
                            Text(discoveredPoint)
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
            listOf()
        )
    )
    val connectionsState: StateFlow<NearbyConnectionsState> = _connectionsState

    init {

    }

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
                object : ConnectionLifecycleCallback() {
                    override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
                        Log.d("startAdvertising", "onConnectionInitiated: $p0")
                    }
                    override fun onConnectionResult(p0: String, p1: ConnectionResolution) {
                        Log.d("startAdvertising", "onConnectionResult: $p0")
                    }
                    override fun onDisconnected(p0: String) {
                        Log.d("startAdvertising", "onDisconnected: $p0")
                    }
                },
                advertisingOptions
            )
            .addOnSuccessListener {
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
                    override fun onEndpointFound(p0: String, p1: DiscoveredEndpointInfo) {
                        Log.d("startDiscovering", "onEndpointFound: $p0")
                        _connectionsState.value = _connectionsState.value.copy(
                            discoveredPoints = _connectionsState.value.discoveredPoints + p1.endpointName
                        )
                    }
                    override fun onEndpointLost(p0: String) {
                        Log.d("startDiscovering", "onEndpointLost: $p0")
                    }
                },
                discoveryOptions
            )
            .addOnSuccessListener {
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
}

data class NearbyConnectionsState(
    val status: String,
    val discoveredPoints: List<String>,
)