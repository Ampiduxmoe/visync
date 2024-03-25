package com.example.visync.ui.screens.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.areNavigationBarsVisible
import androidx.compose.foundation.layout.areStatusBarsVisible
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.visync.ImmersiveModeToggler
import com.example.visync.connections.RunningConnection
import com.example.visync.connections.VisyncNearbyConnections
import com.example.visync.connections.EmptyVisyncNearbyConnectionsListener
import com.example.visync.connections.VisyncNearbyConnectionsConfiguration
import com.example.visync.data.videofiles.Videofile
import com.example.visync.messaging.JsonVisyncMessageConverter
import com.example.visync.messaging.OpenPlayerMessage
import com.example.visync.messaging.SyncBallMessage
import com.example.visync.messaging.TextMessage
import com.example.visync.player.PlayerWrapperPlaybackControls
import com.example.visync.ui.PlaybackSetupOutput
import com.example.visync.ui.components.navigation.GenericAlertDialog
import com.example.visync.ui.components.navigation.VisyncNavigationActions
import com.example.visync.ui.components.navigation.Route
import com.example.visync.ui.components.navigation.MainAppNavigation
import com.example.visync.ui.components.navigation.NavigationType
import com.example.visync.ui.components.navigation.SynchronizeScrolls
import com.example.visync.ui.screens.main.playback_setup.DevicePositionsEditor
import com.example.visync.ui.screens.main.playback_setup.EmptyGuestMessageCallbacks
import com.example.visync.ui.screens.main.playback_setup.EmptyGuestSpecificCallbacks
import com.example.visync.ui.screens.main.playback_setup.EmptyHostSpecificCallbacks
import com.example.visync.ui.screens.main.playback_setup.EmptyPlaybackSetupCallbacks
import com.example.visync.ui.screens.main.playback_setup.EndpointInfo
import com.example.visync.ui.screens.main.playback_setup.GuestConnectionStatus
import com.example.visync.ui.screens.main.playback_setup.PlaybackSetupGuestScreen
import com.example.visync.ui.screens.main.playback_setup.PlaybackSetupHostScreen
import com.example.visync.ui.screens.main.playback_setup.PlaybackSetupUserState
import com.example.visync.ui.screens.main.playback_setup.PlaybackSetupViewModel
import com.example.visync.ui.screens.main.playback_setup.PlaybackSetupViewModelConfiguration
import com.example.visync.ui.screens.main.rooms.RoomsScreen
import com.example.visync.ui.screens.player.VideoConfiguration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainApp(
    windowSize: WindowSizeClass,
    isDarkTheme: Boolean,
    setDarkTheme: (Boolean) -> Unit,
    mainAppUiState: MainAppUiState,
    mainAppNavigationUiState: MainAppNavigationUiState,
    play: (PlaybackStartOptions, PlaybackSetupOutput) -> Unit,
    playbackControls: PlayerWrapperPlaybackControls,
    immersiveModeToggler: ImmersiveModeToggler,
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
    if (!playbackSetupViewModel.isInitialized) {
        val username = mainAppNavigationUiState.editableUsername.value
        val device = mainAppNavigationUiState.editablePhysicalDevice.value
        Log.d("main app", "initializing playback setup viewmodel with $username and $device")
        playbackSetupViewModel.initialize(
            config = PlaybackSetupViewModelConfiguration(
                username = username,
                device = device,
                commonPlaybackSetupCallbacks = EmptyPlaybackSetupCallbacks(),
                hostSpecificCallbacks = EmptyHostSpecificCallbacks(),
                guestSpecificCallbacks = object : EmptyGuestSpecificCallbacks() {
                    override fun onSyncBallMessage(sender: EndpointInfo, message: SyncBallMessage) {
                        Log.d("Test", "syncball received")
                    }

                    override val messages = object : EmptyGuestMessageCallbacks() {
                        override fun onOpenPlayerMessage() {
                            val guestPlaybackSetupState = playbackSetupViewModel.guestPlaybackSetupState.value
                            val playbackSetupOptions = guestPlaybackSetupState.playbackOptions
                            play(
                                PlaybackStartOptions(
                                    videofiles = guestPlaybackSetupState.localSelectedVideofiles,
                                    startFrom = playbackSetupOptions.selectedVideofileIndex,
                                    playbackSpeed = playbackSetupOptions.playbackSpeed
                                ),
                                PlaybackSetupOutput(
                                    videoConfiguration = devicePositionConfigurationToVideoConfiguration(
                                        targetEndpointId = guestPlaybackSetupState.watchers.first().endpointId,
                                        config = playbackSetupViewModel.guestDevicePositions.value!!
                                    ),
                                    isUserHost = false,
                                    resetAllConnections = { playbackSetupViewModel.currentState = PlaybackSetupUserState.IDLE },
                                )
                            )
                        }
                    }
                }
            )
        )
    }
    val hostPlaybackSetupState by playbackSetupViewModel
        .hostPlaybackSetupState.collectAsStateWithLifecycle()
    val hostDevicePositionsState by playbackSetupViewModel
        .hostDevicePositions.collectAsStateWithLifecycle()
    val hostConnectionState by playbackSetupViewModel
        .hostConnectionState.collectAsStateWithLifecycle()

    val guestPlaybackSetupState by playbackSetupViewModel
        .guestPlaybackSetupState.collectAsStateWithLifecycle()
    val guestDevicePositionsState by playbackSetupViewModel
        .guestDevicePositions.collectAsStateWithLifecycle()
    val guestConnectionState by playbackSetupViewModel
        .guestConnectionState.collectAsStateWithLifecycle()


    val navController = rememberNavController()
    val navActions = VisyncNavigationActions(navController)

    MainAppNavigation(
        isDarkTheme = isDarkTheme,
        setDarkTheme = setDarkTheme,
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
                val currentRole = playbackSetupViewModel.currentState

                val notInHostMode = currentRole != PlaybackSetupUserState.HOST
                val isInGuestMode = currentRole == PlaybackSetupUserState.GUEST
                val isConnected = hostConnectionState.allWatcherPings.isNotEmpty() // TODO
                val showConfirmTransitionToHostDialog = isInGuestMode && isConnected
                val transitionToHostRole = {
                    playbackSetupViewModel.currentState = PlaybackSetupUserState.HOST
                }
                if (showConfirmTransitionToHostDialog) {
                    AlertOngoingGuestSetup(
                        onCancel = {
                            navActions.back()
                        },
                        onConfirm = {
                            transitionToHostRole()
                        }
                    )
                    return@composable
                }
                LaunchedEffect(Unit) {
                    if (notInHostMode) {
                        transitionToHostRole()
                    }
                }
                DisposableEffect(Unit) {
                    onDispose {
                        playbackSetupViewModel.hostActions.stopAdvertisingRoom()
                    }
                }
                PlaybackSetupHostScreen(
                    playbackSetupState = hostPlaybackSetupState,
                    hostConnectionState = hostConnectionState,
                    hostActions = playbackSetupViewModel.hostActions,
                    positionsEditor = hostDevicePositionsState,
                    setSelectedVideofiles = playbackSetupViewModel::setSelectedVideofiles,
                    play = {
                        val playbackSetupOptions = hostPlaybackSetupState.playbackOptions
                        playbackSetupViewModel.hostActions.sendOpenPlayer()
                        play(
                            PlaybackStartOptions(
                                videofiles = hostPlaybackSetupState.localSelectedVideofiles,
                                startFrom = playbackSetupOptions.selectedVideofileIndex,
                                playbackSpeed = playbackSetupOptions.playbackSpeed
                            ),
                            PlaybackSetupOutput(
                                videoConfiguration = devicePositionConfigurationToVideoConfiguration(
                                    targetEndpointId = hostPlaybackSetupState.watchers.first().endpointId,
                                    config = hostDevicePositionsState!!
                                ),
                                isUserHost = true,
                                resetAllConnections = { playbackSetupViewModel.currentState = PlaybackSetupUserState.IDLE },
                            )
                        )
                        playbackSetupViewModel.hostActions.stopAdvertisingRoom()
                        playbackSetupViewModel.hostActions.stopPinging()
                   },
                )
            }
            composable(Route.RoomsJoin.routeString) {
                val currentRole = playbackSetupViewModel.currentState

                val guestActions = playbackSetupViewModel.guestActions

                val transitionToGuestRole = {
                    playbackSetupViewModel.currentState = PlaybackSetupUserState.GUEST
                }

                val showConfirmDiscoveryDialog = (
                    currentRole == PlaybackSetupUserState.HOST &&
                    hostConnectionState.hasConnections
                )
                if (showConfirmDiscoveryDialog) {
                    AlertOngoingHostSetup(
                        onCancel = {
                            navActions.back()
                        },
                        onConfirm = {
                            transitionToGuestRole()
                        }
                    )
                    return@composable
                }
                LaunchedEffect(Unit) {
                    if (currentRole != PlaybackSetupUserState.GUEST) {
                        transitionToGuestRole()
                    }
                }
                DisposableEffect(Unit) {
                    onDispose {
                        playbackSetupViewModel.guestActions.stopDiscoveringRooms()
                        playbackSetupViewModel.guestActions.stopPonging()
                    }
                }
                var showPermissionsDialog by remember { mutableStateOf(false) }
                if (showPermissionsDialog) {
                    AllRequiredPermissionsDialog(close = { showPermissionsDialog = false })
                }
                when (guestConnectionState.connectionStatus) {
                    GuestConnectionStatus.IDLE -> {
                        val remainingPermissions = getNotGrantedPermissions()
                        IconButton(
                            onClick = {
                                 if (remainingPermissions.isNotEmpty()) {
                                     showPermissionsDialog = true
                                 } else {
                                    guestActions.startDiscoveringRooms()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "start discovering"
                            )
                        }
                        RoomsScreen(
                            rooms = guestConnectionState.discoveredRooms
                        )
                    }
                    GuestConnectionStatus.DISCOVERING -> {
                        IconButton(
                            onClick = guestActions::stopDiscoveringRooms
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "stop discovering"
                            )
                        }
                        RoomsScreen(
                            rooms = guestConnectionState.discoveredRooms
                        )
                    }
                    GuestConnectionStatus.CONNECTING -> {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Connecting...")
                        }
                        return@composable
                    }
                    GuestConnectionStatus.CONNECTED -> {
                        PlaybackSetupGuestScreen(
                            playbackSetupState = guestPlaybackSetupState,
                            devicePositionsState = guestDevicePositionsState,
                            guestActions = playbackSetupViewModel.guestActions,
                            setSelectedVideofiles = playbackSetupViewModel::setSelectedVideofiles,
                        )
                    }
                    GuestConnectionStatus.CONNECTION_ERROR -> {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Connection error")
                        }
                        return@composable
                    }
                    GuestConnectionStatus.DISCONNECTED -> {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Connection was lost")
                        }
                        return@composable
                    }
                }
            }
            composable(Route.MyProfile.routeString) {
                Column {
                    Spacer(modifier = Modifier.size(64.dp))
                    Row(modifier = Modifier
                        .background(Color.Gray)
                        .height(200.dp)) {
                        Spacer(modifier = Modifier.size(64.dp))

                        val coroutineScope = rememberCoroutineScope()
                        val scrollState1 = rememberScrollState()
                        val scrollState2 = rememberScrollState()
                        val initialOffset by remember { mutableFloatStateOf(0f) }
                        val delta by remember { mutableFloatStateOf(0f) }
                        val prevScroll1 by remember { mutableIntStateOf(scrollState1.value) }
                        val prevScroll2 by remember { mutableIntStateOf(scrollState2.value) }
                        SynchronizeScrolls(scrollState1, scrollState2)
                        Column(modifier = Modifier.width(100.dp)) {
                            Text("%6.2f".format(initialOffset))
                            Text("%6.2f".format(delta))
                            Row {
                                Text(prevScroll1.toString())
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(scrollState1.value.toString())
                            }
                            Row {
                                Text(prevScroll2.toString())
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(scrollState2.value.toString())
                            }
                        }
                        Column(modifier = Modifier
                            .background(Color.Green)
                            .width(40.dp)
                            .height(40.dp)
                            .verticalScroll(scrollState1)
                        ) {
                            for (i in 0..19) {
                                Text(i.toString())
                            }
                        }
                        Column(modifier = Modifier
                            .background(Color.Cyan)
                            .width(40.dp)
                            .height(100.dp)
                            .verticalScroll(scrollState2)
                        ) {
                            for (i in 0..39) {
                                Text(i.toString())
                            }
                        }
                    }
                }
            }
            composable(Route.Friends.routeString) {
                Column {

                }
            }
            composable(Route.RoomsManage.routeString) {
                Column {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val navigationBarsVisible = WindowInsets.areNavigationBarsVisible
                        val statusBarsVisible = WindowInsets.areStatusBarsVisible
                        val navigationBarsHeight = WindowInsets.navigationBars
                        val statusBarsHeight = WindowInsets.statusBars
                        Log.d("fullscreen", "visibility check: (navbar, statusbar) = ($navigationBarsVisible, $statusBarsVisible)")
                        Log.d("fullscreen", "system bars height: (navbar, statusbar) = ($navigationBarsHeight, $statusBarsHeight)")
                        Button(
                            onClick = {
                                immersiveModeToggler.toggle(
//                                    navigationBarsVisible,
//                                    statusBarsVisible
                                )
                            }
                        ) {
                            Text("Toggle fullscreen")
                        }
                    }
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
                val username = mainAppNavigationUiState.editableUsername.value
                val communicationTestViewModel = hiltViewModel<CommunicationTestViewModel>()
                val connectionsWrapper = communicationTestViewModel.visyncNearbyConnections
                connectionsWrapper.initialize(
                    VisyncNearbyConnectionsConfiguration(
                        username = username
                    )
                )
                val messages = rememberSaveable { mutableStateOf(listOf<String>()) }
                val messageConverter = remember { JsonVisyncMessageConverter() }
                val listener = remember { object : EmptyVisyncNearbyConnectionsListener() {
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
                } }
                connectionsWrapper.removeEventListener(listener)
                connectionsWrapper.addEventListener(listener)
                val nearbyConnectionsState by connectionsWrapper
                    .connectionsState.collectAsStateWithLifecycle()
                DisposableEffect(Unit) {
                    onDispose {
                        connectionsWrapper.resetToIdle()
                    }
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("current status = ${nearbyConnectionsState.broadcastingState}")
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row {
                            IconButton(
                                onClick = { connectionsWrapper.startAdvertising() }
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
                                onClick = { connectionsWrapper.startDiscovering() }
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

fun devicePositionConfigurationToVideoConfiguration(
    targetEndpointId: String,
    config: DevicePositionsEditor
): VideoConfiguration {
    val video = config.videoOnEditor
    val devices = config.devicesOnEditor
    val targetDeviceInfo = devices.find { it.watcherInfo.endpointId == targetEndpointId }
    targetDeviceInfo ?: throw IllegalArgumentException("EndpointId was not found in the config")
    val result = VideoConfiguration(
        mmVideoWidth = video.mmWidth,
        mmVideoHeight = video.mmHeight,
        mmDevicePositionX = targetDeviceInfo.displayLeft - video.mmOffsetX,
        mmDevicePositionY = targetDeviceInfo.displayTop - video.mmOffsetY
    )
    Log.d("video info", video.toString())
    Log.d("target device info", targetDeviceInfo.toString())
    Log.d("device config editor to video config", result.toString())
    return VideoConfiguration(
        mmVideoWidth = video.mmWidth,
        mmVideoHeight = video.mmHeight,
        mmDevicePositionX = targetDeviceInfo.displayLeft - video.mmOffsetX,
        mmDevicePositionY = targetDeviceInfo.displayTop - video.mmOffsetY
    )
}

@Composable
fun AllRequiredPermissionsDialog(close: () -> Unit) {
    GenericAlertDialog(onDismissRequest = { close() }) {
        val notGrantedPermissions = getNotGrantedPermissions()
        var remainingPermissions by remember { mutableStateOf(notGrantedPermissions) }
        val askForPermissionsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { grantStatuses ->
                remainingPermissions = remainingPermissions.filter { return@filter !(grantStatuses[it]!!) }
                close()
            }
        )
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "This app requires special permissions to be able to establish wireless connection between several devices",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Row {
//                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        if (remainingPermissions.isNotEmpty()) {
                            Log.d(
                                "permissions",
                                "trying to ask for these permissions:\n${
                                    remainingPermissions.joinToString(separator = "\n")
                                }"
                            )
                            askForPermissionsLauncher.launch(remainingPermissions.toTypedArray())
                        } else {
                            close()
                        }
                    }
                ) {
                    Text("Grant permissions")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "ask for required permissions"
                    )
                }
            }
        }
    }
}

@Composable
fun getNotGrantedPermissions(): List<String> {
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
    val permissionsStatus = requiredPermissions.associateWith {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    val remainingPermissions = permissionsStatus
        .filter { !it.value }
        .map { it.key }
    return remainingPermissions
}

@Composable
@Preview(widthDp=240)
fun AllRequiredPermissionsDialogPreview() {
    AllRequiredPermissionsDialog({})
}