package com.example.visync.ui.screens.main.playback_setup

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.visync.R
import com.example.visync.data.videofiles.Videofile
import com.example.visync.metadata.VideoMetadata
import com.example.visync.ui.components.navigation.getSimplePhysicalDevice
import kotlin.math.max

@Composable
fun PlaybackSetupGuestScreen(
    playbackSetupState: PlaybackSetupState,
    devicePositionsState: DevicePositionsEditor?,
    guestActions: PlaybackSetupGuestActions,
    setSelectedVideofiles: (List<Videofile>) -> Unit,
) {
    val playbackOptions = playbackSetupState.playbackOptions
    val hostAsWatcher = guestActions.getPlaybackSetupHostAsWatcher()
    val meAsWatcher = guestActions.getPlaybackSetupSelfAsWatcher()
    val otherWatchers = playbackSetupState.watchers
        .filter { it != hostAsWatcher && it != meAsWatcher }
    val allWatchers = listOf(
        listOf(hostAsWatcher, meAsWatcher), otherWatchers
    ).flatten()
    val (approvedWatchers, notApprovedWatchers) = allWatchers.partition { it.isApproved }
    val notApprovedWatcherModifier: (Watcher) -> Modifier = { watcher ->
        Modifier
    }
    val approvedWatcherModifier: (Watcher) -> Modifier = { watcher ->
        Modifier
    }

    val setupSelectFilesTab = stringResource(id = R.string.tab_label_setup_select_files)
    val setupSettingsTab = stringResource(id = R.string.tab_label_setup_settings)
    val setupPeopleTab = stringResource(id = R.string.tab_label_setup_people)
    val tabNames = remember {
        listOf(
            setupSelectFilesTab,
            setupSettingsTab,
            setupPeopleTab
        )
    }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val selectedTabName = tabNames[selectedTabIndex]

    val enterTransition = {
        fadeIn()
    }
    val exitTransition = {
        fadeOut()
    }
    Column {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabNames.forEachIndexed { index, tabName ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(tabName) }
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            this@Column.AnimatedVisibility(
                visible = selectedTabName == setupSelectFilesTab,
                enter = enterTransition(),
                exit = exitTransition()
            ) {
                SetupTabSelectFiles(
                    isUserHost = false,
                    localSelectedVideofiles = playbackSetupState.localSelectedVideofiles,
                    playbackOptions = playbackOptions,
                    setSelectedVideofiles = {
                        setSelectedVideofiles(it)
                    },
                    addVideofiles = {
                        setSelectedVideofiles(playbackSetupState.localSelectedVideofiles + it)
                    },
                    removeVideoByMetadata = { metadata ->
                        setSelectedVideofiles(playbackSetupState.localSelectedVideofiles.filter {
                            it.metadata != metadata
                        })
                    },
                    setSelectedVideofileIndex = {

                    },
                    missingFilenames = playbackSetupState.watchers.first().missingVideofileNames
                )
            }
            this@Column.AnimatedVisibility(
                visible = selectedTabName == setupSettingsTab,
                enter = enterTransition(),
                exit = exitTransition()
            ) {
                SetupTabSettings(
                    playbackOptions = playbackOptions
                )
            }
            this@Column.AnimatedVisibility(
                visible = selectedTabName == setupPeopleTab,
                enter = enterTransition(),
                exit = exitTransition()
            ) {
                Column {
                    SetupTabPeople(
                        isUserHost = false,
                        isAdvertising = false,
                        hostAsWatcher = hostAsWatcher,
                        selfAsWatcher = meAsWatcher,
                        allWatchers = allWatchers,
                        watcherPings = null,
                        videoMetadata = playbackSetupState.localSelectedVideofiles.firstOrNull()?.metadata,
                        positionsEditor = devicePositionsState,
                        saveDevicePositions = {},
                        sendSyncBall = { _, _ -> },
                        setGuestCallbacks = guestActions::setGuestSpecificCallbacks,
                        notApprovedWatcherModifier = notApprovedWatcherModifier,
                        approvedWatcherModifier = approvedWatcherModifier,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun PlaybackSetupHostScreen(
    playbackSetupState: PlaybackSetupState,
    hostConnectionState: HostConnectionState,
    hostActions: PlaybackSetupHostActions,
    setSelectedVideofiles: (List<Videofile>) -> Unit,
    positionsEditor: DevicePositionsEditor?,
    play: () -> Unit,
) {
    val allWatchers = playbackSetupState.watchers
    val playbackOptions = playbackSetupState.playbackOptions
    val hostWatcherId = hostActions.getPlaybackSetupHostAsWatcher().endpointId
    val hostAsWatcherQuery = allWatchers.filter { it.endpointId == hostWatcherId }
    if (hostAsWatcherQuery.isEmpty()) {
        return
    }
    val selfWatcherId = hostActions.getPlaybackSetupSelfAsWatcher().endpointId
    val selfWatcherQuery = allWatchers.filter { it.endpointId == selfWatcherId }
    if (selfWatcherQuery.isEmpty()) {
        return
    }
    val hostAsWatcher = hostAsWatcherQuery[0] // TODO: do same in guest setup
    val meAsWatcher = selfWatcherQuery[0]
    val notApprovedWatcherModifier: (Watcher) -> Modifier = { watcher -> // TODO: just pass approve and disapprove / toggle instead of this
        when (watcher.canBeApproved) {
            true -> Modifier.clickable { hostActions.approveWatcher(watcher) }
            false -> Modifier
        }
    }
    val approvedWatcherModifier: (Watcher) -> Modifier = { watcher ->
        when (watcher.canBeApproved) {
            true -> Modifier.clickable { hostActions.disapproveWatcher(watcher) }
            false -> Modifier
        }
    }

    val setupSelectFilesTab = stringResource(id = R.string.tab_label_setup_select_files)
    val setupSettingsTab = stringResource(id = R.string.tab_label_setup_settings)
    val setupPeopleTab = stringResource(id = R.string.tab_label_setup_people)
    val tabNames = remember {
        listOf(
            setupSelectFilesTab,
            setupSettingsTab,
            setupPeopleTab
        )
    }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val selectedTabName = tabNames[selectedTabIndex]

    val enterTransition = {
        fadeIn()
    }
    val exitTransition = {
        fadeOut()
    }
    Column {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabNames.forEachIndexed { index, tabName ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(tabName) }
                )
            }
        }
        Surface(modifier = Modifier.fillMaxSize()) {
            this@Column.AnimatedVisibility(
                visible = selectedTabName == setupSelectFilesTab,
                enter = enterTransition(),
                exit = exitTransition()
            ) {
                SetupTabSelectFiles(
                    isUserHost = true,
                    localSelectedVideofiles = playbackSetupState.localSelectedVideofiles,
                    playbackOptions = playbackOptions,
                    setSelectedVideofiles = {
                        setSelectedVideofiles(it)
                    },
                    addVideofiles = {
                        setSelectedVideofiles(playbackSetupState.localSelectedVideofiles + it)
                    },
                    removeVideoByMetadata = { metadata ->
                        val newFileList = playbackSetupState.localSelectedVideofiles.filter {
                            it.metadata != metadata
                        }
                        if (playbackOptions.selectedVideofileIndex >= newFileList.size) {
                            hostActions.setSelectedVideofileIndex(max(newFileList.size - 1, 0))
                        }
                        setSelectedVideofiles(newFileList)
                    },
                    setSelectedVideofileIndex = {
                        hostActions.setSelectedVideofileIndex(it)
                    },
                    missingFilenames = playbackSetupState.watchers.first().missingVideofileNames
                )
            }
            this@Column.AnimatedVisibility(
                visible = selectedTabName == setupSettingsTab,
                enter = enterTransition(),
                exit = exitTransition()
            ) {
                SetupTabSettings(
                    playbackOptions = playbackOptions,
                    hostActions = hostActions
                )
            }
            this@Column.AnimatedVisibility(
                visible = selectedTabName == setupPeopleTab,
                enter = enterTransition(),
                exit = exitTransition()
            ) {
                SetupTabPeople(
                    isUserHost = true,
                    isAdvertising = hostConnectionState.isAdvertising,
                    startAdvertising = { hostActions.startAdvertisingRoom() },
                    stopAdvertising = { hostActions.stopAdvertisingRoom() },
                    startGroupPlayback = { play() },
                    hostAsWatcher = hostAsWatcher,
                    selfAsWatcher = meAsWatcher,
                    allWatchers = allWatchers.toList(),
                    watcherPings = hostConnectionState.allWatcherPings,
                    videoMetadata = playbackSetupState.localSelectedVideofiles.firstOrNull()?.metadata,
                    positionsEditor = positionsEditor,
                    saveDevicePositions = hostActions::saveDevicePositions,
                    sendSyncBall = hostActions::sendSyncBall,
                    setGuestCallbacks = {},
                    notApprovedWatcherModifier = notApprovedWatcherModifier,
                    approvedWatcherModifier = approvedWatcherModifier,
                    toggleIsApproved = {
                        if (it.isApproved) {
                            hostActions.disapproveWatcher(it)
                        } else {
                            hostActions.approveWatcher(it)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
@Preview
fun PlaybackSetupHostScreenPreview() {
    PlaybackSetupHostScreen(
        playbackSetupState = PlaybackSetupState(
            watchers = getFakeWatchers(),
            playbackOptions = getFakePlaybackOptions(),
            localSelectedVideofiles = getFakeVideofiles()
        ),
        hostConnectionState = HostConnectionState(
            isAdvertising = true,
            allWatcherPings = getFakeWatcherPings()
        ),
        hostActions = getFakePlaybackSetupHostActions(),
        setSelectedVideofiles = {},
        positionsEditor = null,
        play = {},
    )
}

fun getFakeWatchers(): List<Watcher> {
    return listOf(
        Watcher(
            endpointId = "FAKE",
            username = "CoolGuy",
            messagingVersion = null,
            physicalDevice = getSimplePhysicalDevice(),
            isApproved = false,
            missingVideofileNames = emptyList(),
        )
    )
}

fun getFakePlaybackOptions(): PlaybackOptions {
    return PlaybackOptions(
        videofilesMetadata = listOf(
            getFakeVideoMetadata("MyVideo.mp4")
        ),
        selectedVideofileIndex = 0,
        playbackSpeed = 1f,
        repeatMode = Player.REPEAT_MODE_OFF,
    )
}

fun getFakeVideofiles(): List<Videofile> {
    return listOf(
        Videofile(
            uri = Uri.EMPTY,
            metadata = getFakeVideoMetadata("MyVideo.mp4")
        )
    )
}

fun getFakeVideoMetadata(name: String): VideoMetadata {
    return VideoMetadata(
        filename = name,
        duration = 1000L,
        width = 2000f,
        height = 1000f,
    )
}

fun getFakeWatcherPings(): List<EndpointPingData> {
    return listOf(
        EndpointPingData(
            endpointId = "FAKE",
            pingData = PingData(

            )
        )
    )
}

fun getFakePlaybackSetupHostActions(): PlaybackSetupHostActions {
    return object : PlaybackSetupHostActions {
        override fun startAdvertisingRoom() {}
        override fun stopAdvertisingRoom() {}
        override fun stopPinging() {}
        override fun approveWatcher(watcher: Watcher) {}
        override fun disapproveWatcher(watcher: Watcher) {}
        override fun setSelectedVideofileIndex(index: Int) {}
        override fun setPlaybackSpeed(playbackSpeed: Float) {}
        override fun setRepeatMode(repeatMode: @Player.RepeatMode Int) {}
        override fun saveDevicePositions(newValue: DevicePositionsEditor) {}
        override fun sendOpenPlayer() {}
        override suspend fun sendSyncBall(position: Offset, velocity: Offset) {}
        override fun getPlaybackSetupSelfAsWatcher(): Watcher {
            return getFakeWatchers()[0]
        }
        override fun getPlaybackSetupHostAsWatcher(): Watcher {
            return getFakeWatchers()[0]
        }
    }
}