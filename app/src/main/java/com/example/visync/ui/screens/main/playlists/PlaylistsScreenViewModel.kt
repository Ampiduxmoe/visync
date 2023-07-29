package com.example.visync.ui.screens.main.playlists

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.visync.metadata.MetadataReader
import com.example.visync.data.playlists.Playlist
import com.example.visync.data.playlists.PlaylistWithVideofiles
import com.example.visync.data.playlists.PlaylistsRepository
import com.example.visync.data.videofiles.Videofile
import com.example.visync.data.videofiles.VideofilesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class PlaylistsScreenViewModel @Inject constructor(
    private val playlistsRepository: PlaylistsRepository,
    private val videofilesRepository: VideofilesRepository,
    private val metadataReader: MetadataReader,
) : ViewModel() {

    private val debounceDelay = 100L
    private val sharingStopDelay = 5000L

    private var _selectedPlaylistUpdateJob: Job? = null
    private var _selectedPlaylist = MutableStateFlow<PlaylistWithVideofiles?>(null)

    val uiState: StateFlow<PlaylistsUiState>

    private var _playlistOrdering = PlaylistOrdering.ID_ASC
    private val alwaysApplyCorrectOrder = true

    init {
        uiState = playlistsRepository.playlists
            .catch {
                Log.e(
                    PlaylistsScreenViewModel::class.simpleName,
                    "Failed to receive update from repository",
                    it
                )
            }
            .debounce(debounceDelay)
            .combine(_selectedPlaylist) { playlists, selectedPlaylist ->
                selectedPlaylist?.videofiles?.filterNot {
                    metadataReader.isContentUriValid(it.uri)
                }?.let { unavailableVideofiles ->
                    unavailableVideofiles.ifEmpty { return@let }
                    viewModelScope.launch(Dispatchers.IO) {
                        val videofilesAsArray = unavailableVideofiles.toTypedArray()
                        videofilesRepository.removeVideofiles(*videofilesAsArray)
                    }
                }
                PlaylistsUiState(
                    playlists = playlists,
                    selectedPlaylist = selectedPlaylist,
                    loading = false
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(sharingStopDelay),
                PlaylistsUiState(
                    playlists = emptyList(),
                    selectedPlaylist = null,
                    loading = true
                )
            )
    }

    private fun playlistsOrdered(
        playlists: List<Playlist>
    ): List<Playlist> {
        val comparator = getPlaylistComparator<Playlist>(_playlistOrdering)
        // since we should guarantee correct type from corresponding function, suppress
        @Suppress("UNCHECKED_CAST")
        return playlists.sortedWith(comparator as Comparator<in Playlist>)
    }

    private inline fun <reified T>getPlaylistComparator(
        playlistOrdering: PlaylistOrdering
    ): Comparator<*> {
        when (T::class) {
            Playlist::class -> {
                val currentComparison: (Playlist) -> Comparable<*> = {
                    when (playlistOrdering) {
                        PlaylistOrdering.ID_ASC -> { it.playlistId}
                        PlaylistOrdering.NAME_ASC -> { it.name}
                    }
                }
                return compareBy(currentComparison)
            }
            PlaylistWithVideofiles::class -> {
                val currentComparison: (PlaylistWithVideofiles) -> Comparable<*> = {
                    when (playlistOrdering) {
                        PlaylistOrdering.ID_ASC -> { it.playlist.playlistId}
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

    fun addPlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistsRepository.tryAddPlaylist(playlist)
        }
    }

    fun addVideosToPlaylistFromUri(playlist: Playlist, vararg uris: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingUris = videofilesRepository.selectExistingFromUris(*uris)
            val onlyNewUris = uris.filterNot { it in existingUris }
            Log.d(PlaylistsScreenViewModel::class.simpleName, "new uris: $onlyNewUris")
            val uriMetadataList = onlyNewUris.map { uri ->
                uri to metadataReader.getVideoMetadataFromUri(uri)
            }
            val validVideofiles = uriMetadataList
                .filter { it.second != null }
                .map {
                    Videofile(
                        videofileId = 0,
                        uri = it.first,
                        metadata = it.second!!
                    )
                }.toTypedArray()
            Log.d(PlaylistsScreenViewModel::class.simpleName, "valid videos count: ${validVideofiles.size}")
            validVideofiles.ifEmpty { return@launch }
            videofilesRepository.tryAddVideofilesToPlaylist(
                playlist = playlist,
                videofiles = validVideofiles
            )
        }
    }

    fun addFolderToPlaylistFromUri(playlist: Playlist, uri: Uri) {

    }

    fun setSelectedPlaylist(playlistId: Long) {
        _selectedPlaylistUpdateJob?.cancel()
        _selectedPlaylistUpdateJob = viewModelScope.launch(Dispatchers.IO) {
            playlistsRepository.getPlaylistWithVideofiles(playlistId)
                .collect {
                    _selectedPlaylist.value = it
                }
        }
    }

    fun unselectPlaylist() {
        _selectedPlaylistUpdateJob?.cancel()
        _selectedPlaylist.value = null
    }
}

data class PlaylistsUiState(
    val playlists: List<Playlist>,
    val selectedPlaylist: PlaylistWithVideofiles?,
    val loading: Boolean = false,
)

enum class PlaylistOrdering {
    ID_ASC, NAME_ASC
}

// TODO: use SavedStateHandle everywhere to restore state properly
private val SELECTED_PLAYLIST_ID_KEY = "selectedPlaylistId"