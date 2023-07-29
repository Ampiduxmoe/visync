package com.example.visync.player

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

open class DefaultPlayerWrapperPlaybackControls(
    private val player: Player,
    private val playbackState: StateFlow<PlayerWrapperPlaybackState>
) : PlayerWrapperPlaybackControls {

    override fun seekToPrevious() {
        player.seekToPreviousMediaItem()
    }
    override fun seekToNext() {
        player.seekToNextMediaItem()
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
    override fun setRepeatMode(repeatMode: Int) {
        player.repeatMode = repeatMode
    }
    override fun toggleRepeatMode(): @Player.RepeatMode Int {
        val newRepeatMode = (player.repeatMode + 1) % 3
        setRepeatMode(newRepeatMode)
        return newRepeatMode
    }
}