package com.example.visync.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player

data class PlayerWrapperPlaybackState(
    val currentMediaItem: MediaItem?,
    val playerState: @Player.State Int,
    val playWhenReady: Boolean,
    val isPlaying: Boolean,
    val playbackSpeed: Float,
    val currentVideoDuration: Long,
    val currentPosition: Long,
    val currentPositionPollingInterval: Int,
    val repeatMode: @Player.RepeatMode Int,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val volume: Int,
    val muted: Boolean,
)