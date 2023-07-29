package com.example.visync.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

open class DefaultPlayerWrapper @Inject constructor(
    private val player: Player
): PlayerWrapper {
    private val _playbackState = MutableStateFlow(
        PlayerWrapperPlaybackState(
            currentMediaItem = null,
            playerState = Player.STATE_IDLE,
            playWhenReady = false,
            isPlaying = false,
            playbackSpeed = 1f,
            currentVideoDuration = 0,
            currentPosition = 0,
            currentPositionPollingInterval = 125,
            repeatMode = Player.REPEAT_MODE_OFF,
            hasPrevious = false,
            hasNext = false,
            volume = 0,
            muted = false
        )
    )
    override val playbackState: StateFlow<PlayerWrapperPlaybackState> = _playbackState
    override val playbackStateSetters = DefaultPlayerWrapperPlaybackStateSetters(_playbackState)

    override val playbackControls = DefaultPlayerWrapperPlaybackControls(player, playbackState)

    private var isPlayerListenerAdded: Boolean = false
    override var removePlayerListener: () -> Unit = {}

    init {
        player.prepare()
    }

    override fun setPlayerListener(listener: Player.Listener) {
        if (isPlayerListenerAdded) {
            removePlayerListener()
        }
        player.addListener(listener)
        isPlayerListenerAdded = true
        removePlayerListener = { player.removeListener(listener) }
    }

    override fun getPlayer() = player

    override fun buildEventListener(
        setSelectedMediaItem: (MediaItem?) -> Unit,
        onIsPlayingChangedAction: (Boolean) -> Unit,
    ): Player.Listener {
        return DefaultPlayerListener(
            player = player,
            setSelectedMediaItem = setSelectedMediaItem,
            playbackStateSetters = playbackStateSetters,
            onIsPlayingChangedAction = onIsPlayingChangedAction
        )
    }

    override fun setCurrentPositionPollingInterval(interval: Int) {
        _playbackState.value = _playbackState.value.copy(
            currentPositionPollingInterval = interval
        )
    }
}