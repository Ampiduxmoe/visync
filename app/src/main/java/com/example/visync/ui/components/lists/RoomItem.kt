package com.example.visync.ui.components.lists

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.visync.ui.screens.main.playback_setup.DiscoveredRoom

@Composable
fun RoomItem(
    room: DiscoveredRoom,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Text(text = "${room.username} (${room.endpointId})")
    }
}