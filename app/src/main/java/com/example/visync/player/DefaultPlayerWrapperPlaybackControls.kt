package com.example.visync.player

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

class DefaultPlayerWrapperPlaybackControls(
    private val player: Player,
    private val playbackState: StateFlow<PlayerWrapperPlaybackState>
) : PlayerWrapperPlaybackControls {

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