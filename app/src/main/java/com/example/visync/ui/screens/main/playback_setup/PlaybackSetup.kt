package com.example.visync.ui.screens.main.playback_setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.visync.R
import com.example.visync.data.videofiles.Videofile

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
                        meAsWatcher = meAsWatcher,
                        notApprovedWatchers = notApprovedWatchers,
                        approvedWatchers = approvedWatchers,
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
    val playbackOptions = playbackSetupState.playbackOptions
    val hostAsWatcher = hostActions.getPlaybackSetupHostAsWatcher()
    val meAsWatcher = hostActions.getPlaybackSetupSelfAsWatcher()
    val otherWatchers = playbackSetupState.watchers
        .filter { it != hostAsWatcher && it != meAsWatcher }
    val allWatchers = listOf(
        listOf(hostAsWatcher, meAsWatcher), otherWatchers
    ).flatten().toSet()
    val notApprovedWatchers = allWatchers.filter { !it.isApproved }
    val approvedWatchers = allWatchers.filter { it.isApproved }
    val notApprovedWatcherModifier: (Watcher) -> Modifier = { watcher ->
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                        setSelectedVideofiles(it) // TODO
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
                Column {
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        text = "start advertising",
                        modifier = Modifier.clickable { hostActions.startAdvertisingRoom() }
                    )
                    Text(
                        text = "start playback",
                        modifier = Modifier.clickable { play() }
                    )
                    SetupTabPeople(
                        isUserHost = true,
                        isAdvertising = hostConnectionState.isAdvertising,
                        hostAsWatcher = hostAsWatcher,
                        meAsWatcher = meAsWatcher,
                        notApprovedWatchers = notApprovedWatchers,
                        approvedWatchers = approvedWatchers,
                        watcherPings = hostConnectionState.allWatcherPings,
                        videoMetadata = playbackSetupState.localSelectedVideofiles.firstOrNull()?.metadata,
                        positionsEditor = positionsEditor,
                        saveDevicePositions = hostActions::saveDevicePositions,
                        sendSyncBall = hostActions::sendSyncBall,
                        setGuestCallbacks = {},
                        notApprovedWatcherModifier = notApprovedWatcherModifier,
                        approvedWatcherModifier = approvedWatcherModifier,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}