package com.example.visync.data.rooms

import kotlinx.coroutines.flow.StateFlow

interface RoomsRepository {

    val rooms: StateFlow<List<Room>>

    fun getRoom(id: Long): Room?

    fun addRoom(room: Room): Boolean
}