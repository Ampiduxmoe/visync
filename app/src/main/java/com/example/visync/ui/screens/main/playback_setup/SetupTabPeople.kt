package com.example.visync.ui.screens.main.playback_setup

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.visync.R
import com.example.visync.ui.screens.settings.getProfilePreferences

@Composable
fun SetupTabPeople(
    hostAsWatcher: Watcher,
    meAsWatcher: Watcher,
    notApprovedWatchers: List<Watcher>,
    approvedWatchers: List<Watcher>,
    notApprovedWatcherModifier: (Watcher) -> Modifier,
    approvedWatcherModifier: (Watcher) -> Modifier,
    modifier: Modifier = Modifier
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
    Row(modifier = modifier) {
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