package com.example.visync.data.rooms

interface RoomsRepository {

    fun getAllRooms(): List<Room>

    fun getRoom(id: Long): Room?

    fun addRoom(playlist: Room): Boolean
}