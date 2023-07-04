package com.example.visync.ui.screens.main.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.visync.connections.DiscoveredEndpoint
import com.example.visync.connections.VisyncNearbyConnections
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomsScreenViewModel @Inject constructor(
    visyncNearbyConnections: VisyncNearbyConnections
) : ViewModel() {

    private val connectionsDiscoverer = visyncNearbyConnections.asDiscoverer()

    private val defaultUiState = RoomsUiState(
        rooms = listOf()
    )

    private val _uiState = MutableStateFlow(defaultUiState)
    val uiState: StateFlow<RoomsUiState> = _uiState

    init {
        observeDiscovererState()
    }

    @OptIn(FlowPreview::class)
    private fun observeDiscovererState() {
        viewModelScope.launch {
            connectionsDiscoverer.discovererState.debounce(100L).collect {
                _uiState.update { state ->
                    state.copy(
                        rooms = it.discoveredEndpoints
                    )
                }
            }
        }
    }

    fun clearDiscoveredRooms() {
        _uiState.update { defaultUiState }
    }
}

data class RoomsUiState(
    val rooms: List<DiscoveredEndpoint> = emptyList(),
)