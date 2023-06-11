package com.example.visync.ui.screens

import android.net.Uri
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
            selectedVideofile = null
        )
    )
    val uiState: StateFlow<PlayerScreenUiState> = _uiState

    init {
        player.prepare()
    }

    private fun videofileToMediaItem(videofile: Videofile): MediaItem {
        return MediaItem.fromUri(videofile.uri)
    }

    fun setVideofilesToPlay(
        videofilesToPlay: List<Videofile>,
        startFrom: Int,
    ) {
        val selectedVideofile = videofilesToPlay[startFrom]
        val noDummyVideofiles = videofilesToPlay.filter { it.uri != Uri.EMPTY }
        _uiState.value = _uiState.value.copy(
            videofilesToMediaItems = noDummyVideofiles
                .associateWith(::videofileToMediaItem)
                .also { videofilesToMediaItems ->
                    player.setMediaItems(
                        /* mediaItems = */ videofilesToMediaItems.values.toList(),
                        /* startIndex = */ maxOf(noDummyVideofiles.indexOf(selectedVideofile),0),
                        /* startPositionMs = */ 0
                    )
                },
            selectedVideofile = selectedVideofile
        )
    }

    fun getPlayer() = player

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}

data class PlayerScreenUiState(
    val videofilesToMediaItems: Map<Videofile, MediaItem>,
    val selectedVideofile: Videofile?,
)
