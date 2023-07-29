package com.example.visync.player

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

class HostPlayerWrapperPlaybackControls(
    private val player: Player,
    private val playbackState: StateFlow<PlayerWrapperPlaybackState>
): DefaultPlayerWrapperPlaybackControls(player, playbackState) {

    private var playerMessageSender: PlayerMessageSender? = null


    fun setPlayerMessageSender(sender: PlayerMessageSender) {
        playerMessageSender = sender
    }

    fun removePlayerMessageSender() {
        playerMessageSender = null
    }

    override fun seekToPrevious() {
        playerMessageSender?.sendSeekToPrevMessage()
        super.seekToPrevious()
    }
    override fun seekToNext() {
        playerMessageSender?.sendSeekToNextMessage()
        super.seekToNext()
    }
    override fun pause() {
        playerMessageSender?.sendPauseMessage()
        super.pause()
    }
    override fun unpause() {
        playerMessageSender?.sendUnpauseMessage()
        super.unpause()
    }
    override fun seekTo(progress: Float) {
        playerMessageSender?.sendSeekToMessage(progress)
        super.seekTo(progress)
    }
    override fun seekTo(timeMillis: Long) {
        playerMessageSender?.sendSeekToMessage(timeMillis)
        super.seekTo(timeMillis)
    }
}

interface PlayerMessageSender {
    fun sendSeekToPrevMessage()
    fun sendSeekToNextMessage()
    fun sendPauseMessage()
    fun sendUnpauseMessage()
    fun sendSeekToMessage(seekTo: Float)
    fun sendSeekToMessage(seekTo: Long)
}