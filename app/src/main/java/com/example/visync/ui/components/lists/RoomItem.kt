package com.example.visync.ui.components.lists

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.visync.connections.DiscoveredEndpoint

@Composable
fun RoomItem(
    room: DiscoveredEndpoint,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Text(text = "${room.endpointInfo.endpointName} (${room.endpointId})")
    }
}