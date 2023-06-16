package com.example.visync.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

interface PlayerWrapper {
    val playbackState: StateFlow<PlayerWrapperPlaybackState>
    val playbackStateSetters: PlayerWrapperPlaybackStateSetters
    val playbackControls: PlayerWrapperPlaybackControls
    val removePlayerListener: () -> Unit
    fun setPlayerListener(listener: Player.Listener)
    fun buildEventListener(
        setSelectedMediaItem: (MediaItem?) -> Unit,
        onIsPlayingChangedAction: (Boolean) -> Unit = {},
    ): Player.Listener
    fun setCurrentPositionPollingInterval(interval: Int)
    fun getPlayer(): Player
}