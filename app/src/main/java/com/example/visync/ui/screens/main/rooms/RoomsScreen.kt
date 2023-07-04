package com.example.visync.ui.screens.main.rooms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.visync.connections.DiscoveredEndpoint
import com.example.visync.ui.components.lists.RoomItem

@Composable
fun RoomsScreen(
    roomsUiState: RoomsUiState,
    joinRoom: (DiscoveredEndpoint) -> Unit,
) {
    Column {
        val rooms = roomsUiState.rooms
        for (room in rooms) {
            RoomItem(
                room = room,
                modifier = Modifier.clickable {
                    joinRoom(room)
                }
            )
        }
    }
}