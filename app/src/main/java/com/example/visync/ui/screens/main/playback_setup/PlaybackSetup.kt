package com.example.visync.ui.screens.main.playback_setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.visync.R
import com.example.visync.data.videofiles.Videofile

/** PlaybackSetupScreen overload for guest mode */
@Composable
fun PlaybackSetupScreen(
    playbackSetupState: PlaybackSetupState,
    setSelectedVideofilesAsGuest: (List<Videofile>) -> Unit,
) = PlaybackSetupScreen(
    isHostScreen = false,
    playbackSetupState = playbackSetupState,
    playbackSetupOptionSetters = PlaybackSetupOptionSetters(
        setSelectedVideofilesAsGuest,
        {}, {}, {}, {}, {}
    ),
    approveWatcher =  {},
    disapproveWatcher =  {},
    startAdvertising = {},
    play = {},
)

/** PlaybackSetupScreen overload for host mode */
@Composable
fun PlaybackSetupScreen(
    playbackSetupState: PlaybackSetupState,
    playbackSetupOptionSetters: PlaybackSetupOptionSetters,
    approveWatcher: (Watcher) -> Unit,
    disapproveWatcher: (Watcher) -> Unit,
    startAdvertising: () -> Unit,
    play: () -> Unit,
) = PlaybackSetupScreen(
    isHostScreen = true,
    playbackSetupState = playbackSetupState,
    playbackSetupOptionSetters = playbackSetupOptionSetters,
    approveWatcher = approveWatcher,
    disapproveWatcher = disapproveWatcher,
    startAdvertising = startAdvertising,
    play = play
)

@Composable
private fun PlaybackSetupScreen(
    isHostScreen: Boolean,
    playbackSetupState: PlaybackSetupState,
    playbackSetupOptionSetters: PlaybackSetupOptionSetters,
    approveWatcher: (Watcher) -> Unit,
    disapproveWatcher: (Watcher) -> Unit,
    startAdvertising: () -> Unit,
    play: () -> Unit,
) {
    val playbackSetupOptions = playbackSetupState.playbackSetupOptions
    val isUserHost = playbackSetupState.setupMode == SetupMode.HOST
    val isConnecting = playbackSetupState.isConnectingToHost
    val hostAsWatcher = playbackSetupState.hostAsWatcher
    val meAsWatcher = playbackSetupState.meAsWatcher
    val otherWatchers = playbackSetupState.otherWatchers
    val allWatchers = listOf(
        listOf(hostAsWatcher, meAsWatcher), otherWatchers
    ).flatten().toSet()
    val notApprovedWatchers = allWatchers.filter { !it.isApproved }
    val approvedWatchers = allWatchers.filter { it.isApproved }
    val notApprovedWatcherModifier: (Watcher) -> Modifier = { watcher ->
        when (isUserHost) {
            true -> Modifier.clickable { approveWatcher(watcher) }
            false -> Modifier
        }
    }
    val approvedWatcherModifier: (Watcher) -> Modifier = { watcher ->
        when (isUserHost) {
            true -> Modifier.clickable { disapproveWatcher(watcher) }
            false -> Modifier
        }
    }
    if (!isHostScreen && !isUserHost && isConnecting) {
        Box(contentAlignment = Alignment.Center) {
            if (playbackSetupState.connectionError) {
                Text("Connection error.")
            } else {
                Text("Connecting...")
            }
        }
        return
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
                    isUserHost = isUserHost,
                    selectedVideofiles = playbackSetupState.selectedVideofiles,
                    playbackSetupOptions = playbackSetupOptions,
                    setSelectedVideofiles = {
                        playbackSetupOptionSetters.setSelectedVideofiles(it)
                    },
                    addVideofiles = {
                        playbackSetupOptionSetters.addToSelectedVideofiles(it)
                    },
                    setSelectedVideofileIndex = {
                        playbackSetupOptionSetters.setSelectedFileIndex(it)
                    },
                    missingFilenames = playbackSetupState.meAsWatcher.missingVideofileNames
                )
            }
            this@Column.AnimatedVisibility(
                visible = selectedTabName == setupSettingsTab,
                enter = enterTransition(),
                exit = exitTransition()
            ) {
                SetupTabSettings(
                    playbackSetupOptions = playbackSetupOptions,
                    playbackSetupOptionSetters = playbackSetupOptionSetters
                )
            }
            this@Column.AnimatedVisibility(
                visible = selectedTabName == setupPeopleTab,
                enter = enterTransition(),
                exit = exitTransition()
            ) {
                Column {
                    SetupTabPeople(
                        hostAsWatcher = hostAsWatcher,
                        meAsWatcher = meAsWatcher,
                        notApprovedWatchers = notApprovedWatchers,
                        approvedWatchers = approvedWatchers,
                        notApprovedWatcherModifier = notApprovedWatcherModifier,
                        approvedWatcherModifier = approvedWatcherModifier,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isUserHost) {
                        Spacer(modifier = Modifier.height(40.dp))
                        Text(
                            text = "start advertising",
                            modifier = Modifier.clickable { startAdvertising() }
                        )
                        Text(
                            text = "start playback",
                            modifier = Modifier.clickable { play() }
                        )
                    }
                }
            }
        }
    }
}

class PlaybackSetupOptionSetters (
    val setSelectedVideofiles: (List<Videofile>) -> Unit,
    val addToSelectedVideofiles: (List<Videofile>) -> Unit,
    val setSelectedFileIndex: (Int) -> Unit,
    val setDoStream: (Boolean) -> Unit,
    val setPlaybackSpeed: (Float) -> Unit,
    val toggleRepeatMode: () -> Unit,
)