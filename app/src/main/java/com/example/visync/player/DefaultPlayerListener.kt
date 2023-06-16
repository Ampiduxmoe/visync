package com.example.visync.player

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize

class DefaultPlayerListener(
    private val player: Player,
    private val setSelectedMediaItem: (MediaItem?) -> Unit,
    private val playbackStateSetters: PlayerWrapperPlaybackStateSetters,
    private val onIsPlayingChangedAction: (Boolean) -> Unit = {},
) : Player.Listener {

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        Log.d("VisyncPlayerListener", "mediaItem.mediaId=${mediaItem?.mediaId}")
        Log.d("VisyncPlayerListener", "MediaItemTransitionReason=${reason}")
        Log.d("VisyncPlayerListener", "player.hasPrev=${player.hasPreviousMediaItem()}")
        Log.d("VisyncPlayerListener", "player.hasNext=${player.hasNextMediaItem()}")
        setSelectedMediaItem(mediaItem)
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
            playbackStateSetters.setCurrentMediaItem(
                mediaItem = mediaItem,
                currentPosition = player.currentPosition,
                hasPrevious = player.hasPreviousMediaItem(),
                hasNext = player.hasNextMediaItem()
            )
            return
        }
        playbackStateSetters.setCurrentMediaItem(
            mediaItem = mediaItem,
            hasPrevious = player.hasPreviousMediaItem(),
            hasNext = player.hasNextMediaItem()
        )
    }
    override fun onIsLoadingChanged(isLoading: Boolean) {
        Log.d("VisyncPlayerListener", "isLoading=$isLoading")
    }
    override fun onPlaybackStateChanged(playbackState: Int) {
        Log.d("VisyncPlayerListener", "playbackState=$playbackState")
        playbackStateSetters.setPlayerState(playbackState)
        if (playbackState != Player.STATE_READY) {
            return
        }
        Log.d("VisyncPlayerListener", "player.duration=${player.duration}")
        Log.d("VisyncPlayerListener", "player.currentPosition=${player.currentPosition}")
        playbackStateSetters.setDurationAndPosition(player.duration, player.currentPosition)
    }
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        Log.d("VisyncPlayerListener", "playWhenReady=$playWhenReady")
        playbackStateSetters.setPlayWhenReady(playWhenReady)
    }
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        Log.d("VisyncPlayerListener", "isPlaying=$isPlaying")
        playbackStateSetters.setIsPlaying(isPlaying)
        onIsPlayingChangedAction(isPlaying)
    }
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        Log.d("VisyncPlayerListener", "playbackParameters.speed=${playbackParameters.speed}")
        playbackStateSetters.setPlaybackSpeed(playbackParameters.speed)
    }
    override fun onRepeatModeChanged(repeatMode: Int) {
        Log.d("VisyncPlayerListener", "repeatMode=$repeatMode")
        playbackStateSetters.setRepeatMode(repeatMode)
    }
    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
        Log.d("VisyncPlayerListener", "volume=$volume")
        playbackStateSetters.setVolume(volume, muted)
    }
    override fun onVideoSizeChanged(videoSize: VideoSize) {
        Log.d("VisyncPlayerListener", "videoSize=${videoSize.width}w${videoSize.height}h")
    }
    override fun onRenderedFirstFrame() {
        Log.d("VisyncPlayerListener", "onRenderedFirstFrame")
    }
}