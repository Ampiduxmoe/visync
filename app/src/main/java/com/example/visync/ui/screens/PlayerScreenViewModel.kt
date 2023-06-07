package com.example.visync.ui.screens

import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.visync.data.videofiles.Videofile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerScreenViewModel @Inject constructor(
    private val player: Player
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PlayerScreenUiState(
            videofilesToMediaItems = mapOf(),
        )
    )
    val uiState: StateFlow<PlayerScreenUiState> = _uiState

    init {
        player.prepare()
    }

    private fun videofileToMediaItem(videofile: Videofile): MediaItem {
        return MediaItem.fromUri(videofile.uri)
    }

    fun setVideofilesToPlay(videoFilesToPlay: List<Videofile>) {
        _uiState.value = _uiState.value.copy(
            videofilesToMediaItems = videoFilesToPlay
                .associateWith(::videofileToMediaItem)
                .also {
                    player.setMediaItems(it.values.toList())
                }
        )
    }
}

data class PlayerScreenUiState(
    val videofilesToMediaItems: Map<Videofile, MediaItem>,
)
