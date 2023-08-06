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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.visync.connections.RunningConnection
import com.example.visync.connections.VisyncNearbyConnections
import com.example.visync.connections.VisyncNearbyConnectionsListener
import com.example.visync.data.videofiles.Videofile
import com.example.visync.messaging.JsonVisyncMessageConverter
import com.example.visync.messaging.OpenPlayerMessage
import com.example.visync.messaging.TextMessage
import com.example.visync.player.PlayerWrapperPlaybackControls
import com.example.visync.ui.PlaybackSetupOutput
import com.example.visync.ui.components.navigation.VisyncNavigationActions
import com.example.visync.ui.components.navigation.Route
import com.example.visync.ui.components.navigation.MainAppNavigation
import com.example.visync.ui.components.navigation.NavigationType
import com.example.visync.ui.getPlayerMessageSender
import com.example.visync.ui.screens.main.playback_setup.PlaybackSetupOptionSetters
import com.example.visync.ui.screens.main.playback_setup.PlaybackSetupScreen
import com.example.visync.ui.screens.main.playback_setup.PlaybackSetupViewModel
import com.example.visync.ui.screens.main.playback_setup.SetupMode
import com.example.visync.ui.screens.main.rooms.RoomsScreen
import com.example.visync.ui.screens.main.rooms.RoomsScreenViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Composable
fun MainApp(
    windowSize: WindowSizeClass,
    mainAppUiState: MainAppUiState,
    mainAppNavigationUiState: MainAppNavigationUiState,
    play: (PlaybackStartOptions, PlaybackSetupOutput) -> Unit,
    playbackControls: PlayerWrapperPlaybackControls,
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

    val playbackSetupViewModel = hiltViewModel<PlaybackSetupViewModel>()
    val playbackSetupState by playbackSetupViewModel
        .playbackSetupState.collectAsStateWithLifecycle()
    val playbackSetupConnections = playbackSetupViewModel.visyncNearbyConnections
    val connectionsState by playbackSetupConnections
        .connectionsState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navActions = VisyncNavigationActions(navController)

    val playbackSetupOptionSetters = remember {
        PlaybackSetupOptionSetters(
            setSelectedVideofiles = { videofiles ->
                playbackSetupViewModel.setVideofilesAndNotifyIfHost(
                    videofiles = videofiles,
                    startFrom = 0
                )
            },
            addToSelectedVideofiles = { videofiles ->
                playbackSetupViewModel.addVideofilesAndNotifyIfHost(
                    videofiles = videofiles
                )
            },
            setSelectedFileIndex = { index ->
                val currentVideofiles = playbackSetupState.selectedVideofiles
                playbackSetupViewModel.setVideofilesAndNotifyIfHost(
                    videofiles = currentVideofiles,
                    startFrom = index
                )
            },
            setDoStream = { doStream ->
                playbackSetupViewModel.setDoStream(doStream)
            },
            setPlaybackSpeed = { playbackSpeed ->
                playbackSetupViewModel.setPlaybackSpeed(playbackSpeed)
            },
            toggleRepeatMode = {
                val repeatMode = playbackSetupState.playbackSetupOptions.repeatMode
                val newRepeatMode = (repeatMode + 1) % 3
                playbackSetupViewModel.setRepeatMode(newRepeatMode)
            }
        )
    }
    val messageSender = remember {
        getPlayerMessageSender(
            playbackSetupViewModel = playbackSetupViewModel,
            playbackSetupConnections = playbackSetupConnections
        )
    }

    MainAppNavigation(
        uiState = mainAppNavigationUiState,
        navigationType = navigationType,
        navController = navController
    ) {
        NavHost(
            navController = navController,
            startDestination = Route.PlaybackSetup.routeString,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Route.PlaybackSetup.routeString) {
                val username = mainAppNavigationUiState.editableUsername.value
                val context = LocalContext.current

                val isInGuestMode = playbackSetupState.setupMode == SetupMode.GUEST
                val isConnected = connectionsState.runningConnections.isNotEmpty()
                val showConfirmTransitionToHostDialog = isInGuestMode && isConnected
                val transitionToHost = {
                    playbackSetupViewModel.fullResetToHostMode()
                    playbackSetupViewModel.resetMessageEvents()
                }
                if (showConfirmTransitionToHostDialog) {
                    AlertOngoingGuestSetup(
                        onCancel = { navActions.back() },
                        onConfirm = { transitionToHost() }
                    )
                    return@composable
                } else if (isInGuestMode) {
                    transitionToHost()
                }
                PlaybackSetupScreen(
                    playbackSetupState = playbackSetupState,
                    approveWatcher = playbackSetupViewModel::approveWatcher,
                    disapproveWatcher = playbackSetupViewModel::disapproveWatcher,
                    playbackSetupOptionSetters = playbackSetupOptionSetters,
                    startAdvertising =  {
                        playbackSetupConnections.startAdvertising(username, context)
                    },
                    play = {
                        val playbackSetupOptions = playbackSetupState.playbackSetupOptions
                        playbackSetupViewModel.sendOpenPlayer()
                        play(
                            PlaybackStartOptions(
                                videofiles = playbackSetupState.selectedVideofiles,
                                startFrom = playbackSetupOptions.selectedVideofileIndex,
                                playbackSpeed = playbackSetupOptions.playbackSpeed
                            ),
                            PlaybackSetupOutput(
                                isUserHost = true,
                                resetAllConnections = playbackSetupConnections::reset,
                                messageSender = messageSender
                            )
                        )
                        playbackSetupConnections.stopAdvertising()
                   },
                )
            }
            composable(Route.RoomsJoin.routeString) {
                val roomsScreenViewModel = hiltViewModel<RoomsScreenViewModel>()
                val roomsUiState by roomsScreenViewModel
                    .uiState.collectAsStateWithLifecycle()

                val showConfirmDiscoveryDialog = remember { mutableStateOf(false) }

                val username = mainAppNavigationUiState.editableUsername.value
                val context = LocalContext.current
                val transitionToGuestModeAndDiscover = {
                    playbackSetupViewModel.fullResetToGuestMode()
                    playbackSetupConnections.startDiscovering(username, context)
                    playbackSetupViewModel.messageEvents.apply {
                        onOpenPlayerMessage = {
                            val playbackSetupOptions = playbackSetupState.playbackSetupOptions
                            play(
                                PlaybackStartOptions(
                                    videofiles = playbackSetupState.selectedVideofiles,
                                    startFrom = playbackSetupOptions.selectedVideofileIndex,
                                    playbackSpeed = playbackSetupOptions.playbackSpeed
                                ),
                                PlaybackSetupOutput(
                                    isUserHost = false,
                                    resetAllConnections = playbackSetupConnections::reset,
                                    messageSender = messageSender
                                )
                            )
                        }
                        onPauseUnpauseMessage = { message ->
                            when (message.doPause) {
                                true -> { playbackControls.pause() }
                                false -> { playbackControls.unpause() }
                            }
                        }
                    }
                }
                if (showConfirmDiscoveryDialog.value) {
                    AlertOngoingHostSetup(
                        onCancel = {
                            showConfirmDiscoveryDialog.value = false
                        },
                        onConfirm = {
                            transitionToGuestModeAndDiscover()
                            showConfirmDiscoveryDialog.value = false
                        }
                    )
                }

                val isInGuestMode = playbackSetupState.setupMode == SetupMode.GUEST
                val isConnected = connectionsState.runningConnections.isNotEmpty()
                if (isInGuestMode && isConnected) {
                    PlaybackSetupScreen(
                        playbackSetupState = playbackSetupState,
                        setSelectedVideofilesAsGuest = playbackSetupViewModel::setVideofilesAsGuest
                    )
                } else {
                    DisposableEffect(Unit) {
                        onDispose {
                            playbackSetupConnections.stopDiscovering()
                            roomsScreenViewModel.clearDiscoveredRooms()
                        }
                    }

                    if (!connectionsState.isDiscovering) {
                        IconButton(
                            onClick = {
                                val isInHostMode = playbackSetupState.setupMode == SetupMode.HOST
                                val hasConnections = connectionsState.let {
                                    (it.connectionRequests + it.runningConnections).isNotEmpty()
                                }
                                if (isInHostMode && hasConnections) {
                                    showConfirmDiscoveryDialog.value = true
                                } else {
                                    transitionToGuestModeAndDiscover()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "start discovering"
                            )
                        }
                    }
                    RoomsScreen(
                        roomsUiState = roomsUiState,
                        joinRoom = {
                            playbackSetupConnections.stopDiscovering()
                            it.initiateConnection()
                        }
                    )
                }
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
                        Manifest.permission.ACCESS_FINE_LOCATION    to 29..999,
                        Manifest.permission.BLUETOOTH_ADVERTISE     to 31..999,
                        Manifest.permission.BLUETOOTH_CONNECT       to 31..999,
                        Manifest.permission.BLUETOOTH_SCAN          to 31..999,
                        Manifest.permission.NEARBY_WIFI_DEVICES     to 33..999,

                        Manifest.permission.READ_EXTERNAL_STORAGE   to 1..32,
                        Manifest.permission.READ_MEDIA_VIDEO        to 33..999,
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
                                val permissionName = entry.key.substring(
                                    startIndex = entry.key.indexOfLast { it == '.'}
                                )
                                val permissionGrantedStatus = when (entry.value) {
                                    true -> "granted"
                                    false -> "denied"
                                }
                                "$permissionName $permissionGrantedStatus"
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
                val communicationTestViewModel = hiltViewModel<CommunicationTestViewModel>()
                val connectionsWrapper = communicationTestViewModel.visyncNearbyConnections
                val messages = rememberSaveable { mutableStateOf(listOf<String>()) }
                val messageConverter = remember { JsonVisyncMessageConverter() }
                connectionsWrapper.setEventListener(object : VisyncNearbyConnectionsListener() {
                    override fun onNewMessage(message: String, from: RunningConnection) {
                        val fullMessage = messageConverter.decode(message)
                        if (fullMessage is OpenPlayerMessage) {
                            Toast.makeText(
                                /* context = */ context,
                                /* text = */ "playback started!",
                                /* duration = */ Toast.LENGTH_SHORT
                            ).show()
                        }
                        messages.value += "${from.username}: $message"
                    }
                })
                val nearbyConnectionsState by connectionsWrapper
                    .connectionsState.collectAsStateWithLifecycle()
                val username = mainAppNavigationUiState.editableUsername.value
                DisposableEffect(Unit) {
                    onDispose {
                        connectionsWrapper.reset()
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
                                val textMessage = TextMessage(currentMessage.value)
                                val encodedMessage = Json.encodeToString(textMessage)
                                connection.sendMessage(encodedMessage)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertOngoingGuestSetup(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            // Dismiss the dialog when the user clicks outside the dialog or on the back
            // button. If you want to disable that functionality, simply use an empty
            // onDismissRequest.
        }
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "You currently have connections, this will remove your guest setup",
                )
                Row {
                    TextButton(
                        onClick = onCancel
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = onConfirm
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertOngoingHostSetup(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            // Dismiss the dialog when the user clicks outside the dialog or on the back
            // button. If you want to disable that functionality, simply use an empty
            // onDismissRequest.
        }
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "You currently have connections, this will remove your host setup",
                )
                Row {
                    TextButton(
                        onClick = onCancel
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = onConfirm
                    ) {
                        Text("Confirm")
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

class PlaybackStartOptions(
    val videofiles: List<Videofile>,
    val startFrom: Int,
    val playbackSpeed: Float,
)

@HiltViewModel
class CommunicationTestViewModel @Inject constructor(
    val visyncNearbyConnections: VisyncNearbyConnections,
) : ViewModel() {

}