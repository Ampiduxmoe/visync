package com.example.visync.data.rooms

import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale

class FakeRoomsRepository : RoomsRepository {
    private val indexWords: List<String> = listOf(
        "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth"
    )
    private val rooms: MutableList<Room> = (1..9).map {
        Room(
            id = it.toLong(),
            name = "${indexWords[it-1].capitalize(Locale.current)} room"
        )
    }.toMutableList()

    override fun getAllRooms(): List<Room> {
        return rooms
    }

    override fun getRoom(id: Long): Room? {
        return rooms.firstOrNull() { it.id == id }
    }

    override fun addRoom(playlist: Room): Boolean {
        TODO("Not yet implemented")
    }
}