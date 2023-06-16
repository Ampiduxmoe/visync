package com.example.visync.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player

interface PlayerWrapperPlaybackStateSetters {
    fun setCurrentMediaItem(mediaItem: MediaItem?)
    fun setCurrentMediaItem(mediaItem: MediaItem?, hasPrevious: Boolean, hasNext: Boolean)
    fun setCurrentMediaItem(mediaItem: MediaItem?, currentPosition: Long, hasPrevious: Boolean, hasNext: Boolean)
    fun setPlayerState(playerState: @Player.State Int)
    fun setPlayWhenReady(playWhenReady: Boolean)
    fun setIsPlaying(isPlaying: Boolean)
    fun setPlaybackSpeed(playbackSpeed: Float)
    fun setCurrentVideoDuration(videoDuration: Long)
    fun setCurrentPosition(position: Long)
    fun setCurrentPositionPollingInterval(interval: Int)
    fun setDurationAndPosition(videoDuration: Long, position: Long)
    fun setHasPreviousAndNext(hasPrevious: Boolean, hasNext: Boolean)
    fun setRepeatMode(repeatMode: @Player.RepeatMode Int)
    fun setRepeatMode(repeatMode: @Player.RepeatMode Int, hasPrevious: Boolean, hasNext: Boolean)
    fun setVolume(volume: Int, muted: Boolean)
}