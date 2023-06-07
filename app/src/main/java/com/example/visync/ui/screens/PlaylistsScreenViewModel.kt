package com.example.visync.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.visync.data.playlists.Playlist
import com.example.visync.data.playlists.PlaylistWithVideofiles
import com.example.visync.data.playlists.PlaylistsRepository
import com.example.visync.data.videofiles.Videofile
import com.example.visync.data.videofiles.VideofilesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsScreenViewModel @Inject constructor(
    private val playlistsRepository: PlaylistsRepository,
    private val videofilesRepository: VideofilesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistsUiState(loading = true))
    val uiState: StateFlow<PlaylistsUiState> = _uiState

    private var _playlistToVideoFiles: Map<Playlist, List<Videofile>> = emptyMap()

    private var _playlistOrdering = PlaylistOrdering.ID_ASC
    private val alwaysApplyCorrectOrder = true

    init {
        observePlaylists()
        observeVideofiles()
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            playlistsRepository.playlists.collect { playlists ->
                updatePlaylistsMap(
                    playlists = playlists,
                    videofiles = videofilesRepository.videofiles.value,
                    applyCorrectOrdering = alwaysApplyCorrectOrder,
                )
                refreshPlaylistsContent()
            }
        }
    }

    private fun observeVideofiles() {
        viewModelScope.launch {
            videofilesRepository.videofiles.collect { videofiles ->
                updatePlaylistsMap(
                    playlists = playlistsRepository.playlists.value,
                    videofiles = videofiles,
                    applyCorrectOrdering = alwaysApplyCorrectOrder,
                )
                refreshPlaylistsContent()
            }
        }
    }

    private fun updatePlaylistsMap(
        playlists: List<Playlist>,
        videofiles: List<Videofile>,
        @Suppress("SameParameterValue")
        applyCorrectOrdering: Boolean,
    ) {
        _playlistToVideoFiles = mapPlaylistsToVideofiles(
            playlists = playlists,
            videofiles = videofiles,
            applyCorrectOrdering = applyCorrectOrdering
        )
    }

    private fun mapPlaylistsToVideofiles(
        playlists: List<Playlist>,
        videofiles: List<Videofile>,
        applyCorrectOrdering: Boolean,
    ): Map<Playlist, List<Videofile>> {
        val processedPlaylists = if (applyCorrectOrdering) {
            playlistsOrdered(playlists)
        } else {
            playlists
        }
        return processedPlaylists.associateWith { playlist ->
            videofiles.filter { it.playlistId == playlist.id }
        }
    }


    private fun playlistsOrdered(
        playlists: List<Playlist>
    ): List<Playlist> {
        val comparator = getCurrentPlaylistComparator<Playlist>()
        // since we should guarantee correct type from corresponding function, suppress
        @Suppress("UNCHECKED_CAST")
        return playlists.sortedWith(comparator as Comparator<in Playlist>)
    }

    private fun playlistsWithVideofilesOrdered(
        playlists: List<PlaylistWithVideofiles>
    ): List<PlaylistWithVideofiles> {
        val comparator = getCurrentPlaylistComparator<PlaylistWithVideofiles>()
        // since we should guarantee correct type from corresponding function, suppress
        @Suppress("UNCHECKED_CAST")
        return playlists.sortedWith(comparator as Comparator<in PlaylistWithVideofiles>)
    }

    private inline fun <reified T>getCurrentPlaylistComparator(): Comparator<*> {
        when (T::class) {
            Playlist::class -> {
                val currentComparison: (Playlist) -> Comparable<*> = {
                    when (_playlistOrdering) {
                        PlaylistOrdering.ID_ASC -> { it.id}
                        PlaylistOrdering.NAME_ASC -> { it.name}
                    }
                }
                return compareBy(currentComparison)
            }
            PlaylistWithVideofiles::class -> {
                val currentComparison: (PlaylistWithVideofiles) -> Comparable<*> = {
                    when (_playlistOrdering) {
                        PlaylistOrdering.ID_ASC -> { it.playlist.id}
                        PlaylistOrdering.NAME_ASC -> { it.playlist.name}
                    }
                }
                return compareBy(currentComparison)
            }
            else -> {
                throw IllegalArgumentException(
                    """
                        T should be of type ${Playlist::class.simpleName}
                        or ${PlaylistWithVideofiles::class.simpleName},
                        but it was ${T::class.simpleName}
                    """.trimIndent()
                )
            }
        }
    }

    /**
     * Checks whether uiState is in sync with current playlist to videofiles map
     * and if not, updates it.
     */
    fun refreshPlaylistsContent() {
        val newPlaylists = _playlistToVideoFiles.map {
            PlaylistWithVideofiles(
                playlist = it.key,
                videofiles = it.value
            )
        }
        if (newPlaylists == _uiState.value.playlists) {
            return
        }

        val newSelectedPlaylist = _uiState.value.selectedPlaylist?.let { oldSelection ->
            newPlaylists.find { it.playlist.id == oldSelection.playlist.id }
        }

        _uiState.value = PlaylistsUiState(
            playlists = newPlaylists,
            selectedPlaylist = newSelectedPlaylist
        )
    }


    fun setSelectedPlaylist(playlistId: Long) {
        val playlist = uiState.value.playlists.find { it.playlist.id == playlistId }
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
    val playlists: List<PlaylistWithVideofiles> = emptyList(),
    val selectedPlaylist: PlaylistWithVideofiles? = null,
    val loading: Boolean = false,
    val error: String? = null
)

enum class PlaylistOrdering {
    ID_ASC, NAME_ASC
}

// TODO: use SavedStateHandle everywhere to restore state properly
private val SELECTED_PLAYLIST_ID_KEY = "selectedPlaylistId"