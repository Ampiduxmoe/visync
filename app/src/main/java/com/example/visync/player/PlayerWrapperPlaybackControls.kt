package com.example.visync.player

interface PlayerWrapperPlaybackControls {
    fun seekToPrevious()
    fun seekToNext()
    fun pause()
    fun unpause()
    fun seekTo(progress: Float)
    fun seekTo(timeMillis: Long)
    fun setPlaybackSpeed(speed: Float)
    fun toggleRepeatMode()
}