package com.example.visync.data.rooms

import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeRoomsRepository : RoomsRepository {

    private val indexWords: List<String> = listOf(
        "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth"
    )

    private val _rooms = MutableStateFlow<List<Room>>(
        (1..9).map {
            Room(
                endpointId = it.toString(),
                hostUsername = "${indexWords[it-1].capitalize(Locale.current)} room"
            )
        }
    )

    override val rooms: StateFlow<List<Room>> = _rooms

    override fun getRoom(endpointId: String): Room? {
        return _rooms.value.firstOrNull { it.endpointId == endpointId }
    }

    override fun tryAddRoom(room: Room): Boolean {
        if (_rooms.value.contains(room)) {
            return false
        }
        _rooms.value = _rooms.value + room
        return true
    }
}