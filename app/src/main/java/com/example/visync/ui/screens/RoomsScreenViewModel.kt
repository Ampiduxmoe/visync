package com.example.visync.ui.screens

import androidx.lifecycle.ViewModel
import com.example.visync.data.rooms.Room
import com.example.visync.data.rooms.RoomsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RoomsScreenViewModel @Inject constructor() : ViewModel()  {

    @Inject lateinit var roomsRepository: RoomsRepository

    fun getRooms(): List<Room> {
        return roomsRepository.getAllRooms()
    }
}