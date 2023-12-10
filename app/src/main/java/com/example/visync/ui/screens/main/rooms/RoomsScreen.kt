package com.example.visync.ui.screens.main.rooms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.visync.ui.components.lists.RoomItem
import com.example.visync.ui.screens.main.playback_setup.DiscoveredRoom

@Composable
fun RoomsScreen(
    rooms: List<DiscoveredRoom>,
) {
    Column {
        for (room in rooms) {
            RoomItem(
                room = room,
                modifier = Modifier.clickable {
                    room.connect()
                }
            )
        }
    }
}