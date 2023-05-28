package com.example.visync.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.visync.data.playlists.Playlist
import com.example.visync.data.playlists.PlaylistsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsScreenViewModel @Inject constructor(
    public val playlistsRepository: PlaylistsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistsUiState(loading = true))
    val uiState: StateFlow<PlaylistsUiState> = _uiState

    init {
        observePlaylists()
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            playlistsRepository.playlists.collect { playlists ->
                _uiState.value = PlaylistsUiState(playlists = playlists)
            }
        }
    }

    fun setSelectedPlaylist(playlistId: Long, displayMode: PlaylistsDisplayMode) {
        val playlist = uiState.value.playlists.find { it.id == playlistId }
        _uiState.value = _uiState.value.copy(
            selectedPlaylist = playlist,
        )
    }

    fun closeDetailScreen() {
        _uiState.value = _uiState
            .value.copy(
                selectedPlaylist = null,
            )
    }
}

data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val selectedPlaylist: Playlist? = null,
    val loading: Boolean = false,
    val error: String? = null
)

enum class PlaylistsDisplayMode {
    SINGLE_COLUMN, DUAL_COLUMN
}
