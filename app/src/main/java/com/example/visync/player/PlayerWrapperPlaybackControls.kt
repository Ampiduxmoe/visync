package com.example.visync.player

import androidx.media3.common.Player

interface PlayerWrapperPlaybackControls {
    fun seekToPrevious()
    fun seekToNext()
    fun pause()
    fun unpause()
    fun seekTo(progress: Float)
    fun seekTo(timeMillis: Long)
    fun setPlaybackSpeed(speed: Float)
    fun setRepeatMode(repeatMode: @Player.RepeatMode Int)
    fun toggleRepeatMode(): @Player.RepeatMode Int
}