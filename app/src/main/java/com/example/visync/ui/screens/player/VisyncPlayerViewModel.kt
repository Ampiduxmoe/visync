package com.example.visync.ui.screens.player

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import com.example.visync.data.videofiles.Videofile
import com.example.visync.player.PlayerWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class VisyncPlayerViewModel @Inject constructor(
    val playerWrapper: PlayerWrapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VisyncPlayerUiState(
            currentPlaylist = mapOf(),
            selectedVideofile = null
        )
    )
    val uiState: StateFlow<VisyncPlayerUiState> = _uiState

    private val mainHandler = Handler(Looper.getMainLooper())
    private val updateCurrentPositionTask = object : Runnable {
        override fun run() {
            val pollingInterval = playerWrapper.playbackState.value.currentPositionPollingInterval
            playerWrapper.playbackStateSetters.setCurrentPosition(
                playerWrapper.getPlayer().currentPosition
            )
            mainHandler.postDelayed(this, pollingInterval.toLong())
        }
    }

    init {
        val eventListener = playerWrapper.buildEventListener(
            setSelectedMediaItem = this::setSelectedVideofileByMediaItem,
            onIsPlayingChangedAction = {isPlaying ->
                if (isPlaying) {
                    mainHandler.post(updateCurrentPositionTask)
                } else {
                    mainHandler.removeCallbacks(updateCurrentPositionTask)
                }
            }
        )
        playerWrapper.setPlayerListener(eventListener)
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
            currentPlaylist = noDummyVideofiles
                .associateWith(::videofileToMediaItem)
                .also { videofilesToMediaItems ->
                    playerWrapper.getPlayer().setMediaItems(
                        /* mediaItems = */ videofilesToMediaItems.values.toList(),
                        /* startIndex = */ maxOf(noDummyVideofiles.indexOf(selectedVideofile),0),
                        /* startPositionMs = */ 0
                    )
                },
            selectedVideofile = selectedVideofile
        )
    }

    private fun setSelectedVideofileByMediaItem(mediaItem: MediaItem?) {
        val newSelectedVideofile = _uiState.value
            .currentPlaylist
            .filter { it.value == mediaItem }
            .keys
            .firstOrNull()
        if (_uiState.value.selectedVideofile == newSelectedVideofile) {
            return
        }
        _uiState.value = _uiState.value.copy(
            selectedVideofile = newSelectedVideofile
        )
    }

    override fun onCleared() {
        super.onCleared()
        playerWrapper.removePlayerListener()
        playerWrapper.getPlayer().release()
    }
}

data class VisyncPlayerUiState(
    val currentPlaylist: Map<Videofile, MediaItem>,
    val selectedVideofile: Videofile?,
)