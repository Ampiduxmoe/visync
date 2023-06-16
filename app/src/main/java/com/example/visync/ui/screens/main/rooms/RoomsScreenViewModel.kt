package com.example.visync.ui.screens.main.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.visync.data.rooms.Room
import com.example.visync.data.rooms.RoomsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomsScreenViewModel @Inject constructor(
    private val roomsRepository: RoomsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoomsUiState(loading = true))
    val uiState: StateFlow<RoomsUiState> = _uiState

    init {
        observeRooms()
    }

    private fun observeRooms() {
        viewModelScope.launch {
            roomsRepository.rooms.collect { rooms ->
                _uiState.value = RoomsUiState(rooms = rooms)
            }
        }
    }

    fun setSelectedRoom(roomId: Long) {
        val room = uiState.value.rooms.find { it.id == roomId }
        _uiState.value = _uiState.value.copy(
            selectedRoom = room,
        )
    }

    fun closeDetailScreen() {
        _uiState.value = _uiState
            .value.copy(
                selectedRoom = null,
            )
    }
}

data class RoomsUiState(
    val rooms: List<Room> = emptyList(),
    val selectedRoom: Room? = null,
    val loading: Boolean = false,
    val error: String? = null
)