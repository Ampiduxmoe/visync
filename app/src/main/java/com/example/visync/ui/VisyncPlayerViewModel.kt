package com.example.visync.ui

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import com.example.visync.data.videofiles.Videofile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class VisyncPlayerViewModel @Inject constructor(
    private val player: Player
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VisyncPlayerUiState(
            currentPlaylist = mapOf(),
            selectedVideofile = null
        )
    )
    val uiState: StateFlow<VisyncPlayerUiState> = _uiState

    private val _playbackState = MutableStateFlow(
        VisyncPlayerPlaybackState(
            currentMediaItem = null,
            playerState = Player.STATE_IDLE,
            playWhenReady = false,
            playbackSpeed = 1f,
            currentVideoDuration = 0,
            currentPosition = 0,
            repeatMode = Player.REPEAT_MODE_OFF,
            hasPrevious = false,
            hasNext = false,
            volume = 0,
            muted = false
        )
    )
    val playbackState: StateFlow<VisyncPlayerPlaybackState> = _playbackState

    val playbackControls = buildVisyncPlayerPlaybackControls(player, playbackState)

    private var isPlayerListenerAdded: Boolean = false
    private var removePlayerListener: () -> Unit

    private val mainHandler = Handler(Looper.getMainLooper())
    private val updateCurrentPositionTask = object : Runnable {
        override fun run() {
            _playbackState.value = playbackState.value.copy(
                currentPosition = player.currentPosition
            )
            mainHandler.postDelayed(this, 1000)
        }
    }

    init {
        val eventListener = buildVisyncPlayerEventListener(
            player = player,
            uiStateSetters = buildUiStateSetters(_uiState),
            playbackStateSetters = buildPlaybackStateSetters(_playbackState),
            onIsPlayingChanged = { isPlaying ->
                if (isPlaying) {
                    mainHandler.post(updateCurrentPositionTask)
                } else {
                    mainHandler.removeCallbacks(updateCurrentPositionTask)
                }
            }
        )
        setPlayerListener(eventListener)
        removePlayerListener = { player.removeListener(eventListener) }

        player.prepare()
    }

    private fun setPlayerListener(listener: Player.Listener) {
        if (isPlayerListenerAdded) {
            removePlayerListener()
        }
        player.addListener(listener)
        isPlayerListenerAdded = true
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
        removePlayerListener()
        player.release()
    }
}

data class VisyncPlayerUiState(
    val currentPlaylist: Map<Videofile, MediaItem>,
    val selectedVideofile: Videofile?,
)

interface VisyncPlayerUiStateSetters {
    fun setSelectedVideofileByMediaItem(mediaItem: MediaItem?)
}

private fun buildUiStateSetters(
    uiState: MutableStateFlow<VisyncPlayerUiState>
) = object : VisyncPlayerUiStateSetters {

    override fun setSelectedVideofileByMediaItem(mediaItem: MediaItem?) {
        val newSelectedVideofile = uiState.value
            .currentPlaylist
            .filter { it.value == mediaItem }
            .keys
            .firstOrNull()
        if (uiState.value.selectedVideofile == newSelectedVideofile) {
            return
        }
        uiState.value = uiState.value.copy(
            selectedVideofile = newSelectedVideofile
        )
    }
}

data class VisyncPlayerPlaybackState(
    val currentMediaItem: MediaItem?,
    val playerState: @Player.State Int,
    val playWhenReady: Boolean,
    val playbackSpeed: Float,
    val currentVideoDuration: Long,
    val currentPosition: Long,
    val repeatMode: @Player.RepeatMode Int,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val volume: Int,
    val muted: Boolean,
)

interface VisyncPlayerPlaybackStateSetters {
    fun setCurrentMediaItem(mediaItem: MediaItem?)
    fun setCurrentMediaItem(mediaItem: MediaItem?, hasPrevious: Boolean, hasNext: Boolean)
    fun setPlayerState(playerState: @Player.State Int)
    fun setPlayWhenReady(playWhenReady: Boolean)
    fun setPlaybackSpeed(playbackSpeed: Float)
    fun setCurrentVideoDuration(videoDuration: Long)
    fun setCurrentPosition(position: Long)
    fun setDurationAndPosition(videoDuration: Long, position: Long)
    fun setHasPreviousAndNext(hasPrevious: Boolean, hasNext: Boolean)
    fun setRepeatMode(repeatMode: @Player.RepeatMode Int)
    fun setRepeatMode(repeatMode: @Player.RepeatMode Int, hasPrevious: Boolean, hasNext: Boolean)
    fun setVolume(volume: Int, muted: Boolean)
}

private fun buildPlaybackStateSetters(
    playbackState: MutableStateFlow<VisyncPlayerPlaybackState>
) = object : VisyncPlayerPlaybackStateSetters {
    override fun setCurrentMediaItem(mediaItem: MediaItem?) {
        playbackState.value = playbackState.value.copy(
            currentMediaItem = mediaItem
        )
    }
    override fun setCurrentMediaItem(
        mediaItem: MediaItem?,
        hasPrevious: Boolean,
        hasNext: Boolean
    ) {
        playbackState.value = playbackState.value.copy(
            currentMediaItem = mediaItem,
            hasPrevious = hasPrevious,
            hasNext = hasNext
        )
    }
    override fun setPlayerState(playerState: @Player.State Int) {
        playbackState.value = playbackState.value.copy(
            playerState = playerState
        )
    }
    override fun setPlayWhenReady(playWhenReady: Boolean) {
        playbackState.value = playbackState.value.copy(
            playWhenReady = playWhenReady
        )
    }
    override fun setPlaybackSpeed(playbackSpeed: Float) {
        playbackState.value = playbackState.value.copy(
            playbackSpeed = playbackSpeed
        )
    }
    override fun setCurrentVideoDuration(videoDuration: Long) {
        playbackState.value = playbackState.value.copy(
            currentVideoDuration = videoDuration
        )
    }
    override fun setCurrentPosition(position: Long) {
        playbackState.value = playbackState.value.copy(
            currentPosition = position
        )
    }
    override fun setDurationAndPosition(videoDuration: Long, position: Long) {
        val isDurationChanged = playbackState.value.currentVideoDuration != videoDuration
        val isPositionChanged = playbackState.value.currentPosition != position
        if (isDurationChanged && isPositionChanged) {
            playbackState.value = playbackState.value.copy(
                currentVideoDuration = videoDuration,
                currentPosition = position
            )
            return
        }
        if (isDurationChanged) {
            setCurrentVideoDuration(videoDuration)
            return
        }
        if (isPositionChanged) {
            setCurrentPosition(position)
            return
        }
    }
    override fun setHasPreviousAndNext(hasPrevious: Boolean, hasNext: Boolean) {
        playbackState.value = playbackState.value.copy(
            hasPrevious = hasPrevious,
            hasNext = hasNext
        )
    }
    override fun setRepeatMode(repeatMode: @Player.RepeatMode Int) {
        playbackState.value = playbackState.value.copy(
            repeatMode = repeatMode
        )
    }
    override fun setRepeatMode(repeatMode: Int, hasPrevious: Boolean, hasNext: Boolean) {
        playbackState.value = playbackState.value.copy(
            repeatMode = repeatMode,
            hasPrevious = hasPrevious,
            hasNext = hasNext
        )
    }
    override fun setVolume(volume: Int, muted: Boolean) {
        playbackState.value = playbackState.value.copy(
            volume = volume,
            muted = muted
        )
    }
}

private fun buildVisyncPlayerEventListener(
    player: Player,
    uiStateSetters: VisyncPlayerUiStateSetters,
    playbackStateSetters: VisyncPlayerPlaybackStateSetters,
    onIsPlayingChanged: (Boolean) -> Unit = {},
): Player.Listener {
    return object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Log.d("VisyncPlayerListener", "mediaItem.mediaId=${mediaItem?.mediaId}")
            Log.d("VisyncPlayerListener", "player.hasPrev=${player.hasPreviousMediaItem()}")
            Log.d("VisyncPlayerListener", "player.hasNext=${player.hasNextMediaItem()}")
            uiStateSetters.setSelectedVideofileByMediaItem(mediaItem)
            playbackStateSetters.setCurrentMediaItem(
                mediaItem = mediaItem,
                hasPrevious = player.hasPreviousMediaItem(),
                hasNext = player.hasNextMediaItem()
            )
        }
        override fun onIsLoadingChanged(isLoading: Boolean) {
            Log.d("VisyncPlayerListener", "isLoading=$isLoading")
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d("VisyncPlayerListener", "playbackState=$playbackState")
            playbackStateSetters.setPlayerState(playbackState)
            if (playbackState != Player.STATE_READY) {
                return
            }
            Log.d("VisyncPlayerListener", "player.duration=${player.duration}")
            Log.d("VisyncPlayerListener", "player.currentPosition=${player.currentPosition}")
            playbackStateSetters.setDurationAndPosition(player.duration, player.currentPosition)
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d("VisyncPlayerListener", "isPlaying=$isPlaying")
            onIsPlayingChanged(isPlaying)
        }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            Log.d("VisyncPlayerListener", "playWhenReady=$playWhenReady")
            playbackStateSetters.setPlayWhenReady(playWhenReady)
        }
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            Log.d("VisyncPlayerListener", "playbackParameters.speed=${playbackParameters.speed}")
            playbackStateSetters.setPlaybackSpeed(playbackParameters.speed)
        }
        override fun onRepeatModeChanged(repeatMode: Int) {
            Log.d("VisyncPlayerListener", "repeatMode=$repeatMode")
            playbackStateSetters.setRepeatMode(repeatMode)
        }
        override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
            Log.d("VisyncPlayerListener", "volume=$volume")
            playbackStateSetters.setVolume(volume, muted)
        }
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            Log.d("VisyncPlayerListener", "videoSize=${videoSize.width}w${videoSize.height}h")
        }
        override fun onRenderedFirstFrame() {
            Log.d("VisyncPlayerListener", "onRenderedFirstFrame")
        }
    }
}

interface VisyncPlayerPlaybackControls {
    fun seekToPrevious()
    fun seekToNext()
    fun pause()
    fun unpause()
    fun seekTo(progress: Float)
    fun seekTo(timeMillis: Long)
    fun setPlaybackSpeed(speed: Float)
    fun toggleRepeatMode()
}

private fun buildVisyncPlayerPlaybackControls(
    player: Player,
    playbackState: StateFlow<VisyncPlayerPlaybackState>
) = object : VisyncPlayerPlaybackControls {

    override fun seekToPrevious() {
        player.seekToPrevious()
    }
    override fun seekToNext() {
        player.seekToNext()
    }
    override fun pause() {
        player.playWhenReady = false
    }
    override fun unpause() {
        player.playWhenReady = true
    }
    override fun seekTo(progress: Float) {
        val videoDuration = playbackState.value.currentVideoDuration
        val timeMillis = (videoDuration * progress).toLong()
        seekTo(timeMillis)
    }
    override fun seekTo(timeMillis: Long) {
        val videoDuration = playbackState.value.currentVideoDuration
        player.seekTo(timeMillis.coerceAtMost(videoDuration))
    }
    override fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed.coerceIn(0.1f, 2f))
    }
    override fun toggleRepeatMode() {
        player.repeatMode = (player.repeatMode + 1) % 3
    }
}