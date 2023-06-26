package com.example.visync.ui.screens.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.visync.connections.RunningConnection

@Composable
fun PlaybackSetupScreen(
    playbackSetupState: PlaybackSetupState,
    connectedUsers: List<RunningConnection>,
    startPlaying: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "start playback",
            modifier = Modifier.clickable { startPlaying() }
        )
        Text("connected users:")
        for (connection in connectedUsers) {
            Text(connection.endpointUsername)
        }
    }
}