package com.example.visync.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.example.visync.ui.components.lists.RoomItem

@Composable
fun RoomsScreen(
    roomsScreenViewModel: RoomsScreenViewModel
) {
    Column {
        val rooms = roomsScreenViewModel.getRooms()
        for (room in rooms) {
            RoomItem(room)
        }
    }
}