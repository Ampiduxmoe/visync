package com.example.visync.ui.screens.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.visync.connections.RunningConnection
import com.example.visync.connections.VisyncNearbyConnectionsImpl
import com.example.visync.connections.VisyncNearbyConnectionsListener
import com.example.visync.data.playlists.PlaylistWithVideofiles
import com.example.visync.ui.components.navigation.Route
import com.example.visync.ui.components.navigation.MainAppNavigation
import com.example.visync.ui.components.navigation.NavigationType
import com.example.visync.ui.screens.main.playlists.PlaylistsScreen
import com.example.visync.ui.screens.main.playlists.PlaylistsScreenViewModel
import com.example.visync.ui.screens.main.rooms.RoomsScreen
import com.example.visync.ui.screens.main.rooms.RoomsScreenViewModel
import com.google.android.gms.nearby.Nearby

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
                val context = LocalContext.current
                val connectionsWrapper = VisyncNearbyConnectionsImpl(
                    Nearby.getConnectionsClient(context)
                )
                val messages = remember { mutableStateOf(listOf<String>()) }
                connectionsWrapper.setEventListener(object : VisyncNearbyConnectionsListener() {
                    override fun onNewMessage(message: String, from: RunningConnection) {
                        if (message == "playback start") {
                            Toast.makeText(
                                /* context = */ context,
                                /* text = */ "playback started!",
                                /* duration = */ Toast.LENGTH_SHORT
                            ).show()
                        }
                        messages.value += "${from.endpointUsername}: $message"
                    }
                })
                val nearbyConnectionsState by connectionsWrapper
                    .connectionsState.collectAsStateWithLifecycle()
                val username = mainAppNavigationUiState.editableUsername.value
                DisposableEffect(Unit) {
                    onDispose {
                        connectionsWrapper.stop()
                    }
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("current status = ${nearbyConnectionsState.status}")
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row {
                            IconButton(
                                onClick = { connectionsWrapper.startAdvertising(username, context) }
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
                                onClick = connectionsWrapper::stopAdvertising
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
                                onClick = { connectionsWrapper.startDiscovering(username, context) }
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
                                onClick = connectionsWrapper::stopDiscovering
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
                            val connection = nearbyConnectionsState.runningConnections.firstOrNull()
                            connection?.let {
                                connection.sendMessage(currentMessage.value)
                                messages.value += "you: ${currentMessage.value}"
                                currentMessage.value = ""
                            }
                        }
                    ) {
                        Text("send this message!")
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp)
                    ) {
                        items(messages.value) {msg ->
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

enum class ContentDisplayMode {
    SINGLE_COLUMN, DUAL_COLUMN
}

data class PlaybackStartOptions(
    val playlist: PlaylistWithVideofiles,
    val startFrom: Int,
    val playbackMode: VisyncPlaybackMode,
)

enum class VisyncPlaybackMode {
    ALONE, GROUP
}