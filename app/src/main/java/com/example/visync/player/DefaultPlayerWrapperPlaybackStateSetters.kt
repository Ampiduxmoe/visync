package com.example.visync.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow

class DefaultPlayerWrapperPlaybackStateSetters(
    private val playbackState: MutableStateFlow<PlayerWrapperPlaybackState>
) : PlayerWrapperPlaybackStateSetters {

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
    override fun setCurrentMediaItem(
        mediaItem: MediaItem?,
        currentPosition: Long,
        hasPrevious: Boolean,
        hasNext: Boolean
    ) {
        playbackState.value = playbackState.value.copy(
            currentMediaItem = mediaItem,
            currentPosition = currentPosition,
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
    override fun setIsPlaying(isPlaying: Boolean) {
        playbackState.value = playbackState.value.copy(
            isPlaying = isPlaying
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
    override fun setCurrentPositionPollingInterval(interval: Int) {
        playbackState.value = playbackState.value.copy(
            currentPositionPollingInterval = interval
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