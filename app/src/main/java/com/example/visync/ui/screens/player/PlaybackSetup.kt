package com.example.visync.ui.screens.player

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.visync.R
import com.example.visync.ui.screens.settings.getProfilePreferences

@Composable
fun PlaybackSetupScreen(
    playbackSetupState: PlaybackSetupState,
    approveWatcher: (Watcher) -> Unit,
    disapproveWatcher: (Watcher) -> Unit,
    openPlayer: () -> Unit,
) {
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
    if (!isUserHost && isConnecting) {
        Box(contentAlignment = Alignment.Center) {
            if (playbackSetupState.connectionError) {
                Text("Connection error.")
            } else {
                Text("Connecting...")
            }
        }
        return
    }
    Column {
        if (isUserHost) {
            Text(
                text = "start playback",
                modifier = Modifier.clickable { openPlayer() }
            )
        }
        WatchersBlock(
            hostAsWatcher = hostAsWatcher,
            meAsWatcher = meAsWatcher,
            notApprovedWatchers = notApprovedWatchers,
            approvedWatchers = approvedWatchers,
            notApprovedWatcherModifier = notApprovedWatcherModifier,
            approvedWatcherModifier = approvedWatcherModifier
        )
        if (!isUserHost) {
            return
        }
    }
}

@Composable
fun WatchersBlock(
    hostAsWatcher: Watcher,
    meAsWatcher: Watcher,
    notApprovedWatchers: List<Watcher>,
    approvedWatchers: List<Watcher>,
    notApprovedWatcherModifier: (Watcher) -> Modifier,
    approvedWatcherModifier: (Watcher) -> Modifier,
) {
    val context = LocalContext.current
    val watcherColor: (Watcher) -> Color = { watcher ->
        when (watcher.messagingVersion != meAsWatcher.messagingVersion) {
            true -> Color.Red // versions conflict
            false -> Color.Unspecified // versions match
        }
    }
    val watcherText: (Watcher) -> String = { watcher ->
        if (watcher == meAsWatcher && meAsWatcher.username.isEmpty()) {
            val backupUsername = getUsername(context)
            val maybeHost = if (hostAsWatcher.username == backupUsername) " (host)" else "" // redo
            "$backupUsername$maybeHost (me)"
        } else {
            val maybeHost = if (watcher == hostAsWatcher) " (host)" else ""
            val maybeMe = if (watcher == meAsWatcher) " (me)" else ""
            "${watcher.username} [${watcher.endpointId}]$maybeHost$maybeMe"
        }
    }
    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            Text("not approved watchers:")
            for (watcher in notApprovedWatchers) {
                Text(
                    text = watcherText(watcher),
                    color = watcherColor(watcher),
                    modifier = notApprovedWatcherModifier(watcher)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("approved watchers:")
            for (watcher in approvedWatchers) {
                Text(
                    text = watcherText(watcher),
                    color = watcherColor(watcher),
                    modifier = approvedWatcherModifier(watcher)
                )
            }
        }
    }
}

private fun getUsername(context: Context): String? {
    val profilePrefs = getProfilePreferences(context)
    val usernameKey = context.getString(R.string.prefs_profile_username)
    return profilePrefs.getString(usernameKey, null)
}