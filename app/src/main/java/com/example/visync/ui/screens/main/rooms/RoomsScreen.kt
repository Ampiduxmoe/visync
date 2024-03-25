package com.example.visync.ui.screens.main.rooms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.visync.ui.components.lists.RoomItem
import com.example.visync.ui.screens.main.playback_setup.DiscoveredRoom

@Composable
fun RoomsScreen(
    rooms: List<DiscoveredRoom>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        for (room in rooms) {
            RoomItem(
                room = room,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { room.connect() }
            )
        }
    }
}

@Composable
@Preview
fun RoomsScreenPreview() {
    RoomsScreen(
        listOf(
            object : DiscoveredRoom {
                override val endpointId = "FRST"
                override val username = "CoolGuy"
                override fun connect() {}
            },
            object : DiscoveredRoom {
                override val endpointId = "SCND"
                override val username = "GenericGuy"
                override fun connect() {}
            },
            object : DiscoveredRoom {
                override val endpointId = "THRD"
                override val username = "FabulousCapybara"
                override fun connect() {}
            }
        )
    )
}