package com.example.visync.ui.components.lists

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.visync.data.rooms.Room

@Composable
fun RoomItem(
    room: Room
) {
    Row {
        Text(text = room.name)
    }
}