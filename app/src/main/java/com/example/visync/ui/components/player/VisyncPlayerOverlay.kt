package com.example.visync.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import com.example.visync.data.videofiles.Videofile
import com.example.visync.player.PlayerWrapperPlaybackControls
import com.example.visync.player.PlayerWrapperPlaybackState
import com.example.visync.ui.PlayerMessageSender

@Composable
fun VisyncPlayerOverlay(
    selectedVideofile: Videofile?,
    playbackState: PlayerWrapperPlaybackState,
    playbackControls: PlayerWrapperPlaybackControls,
    isUserHost: Boolean,
    messageSender: PlayerMessageSender,
    onOverlayClicked: () -> Unit,
    closePlayer: () -> Unit,
    modifier: Modifier = Modifier,
    disableAutoHiding: () -> Unit = {},
    enableAutoHiding: () -> Unit = {},
) {
    val backgroundModifier = Modifier.background(
        brush = Brush.linearGradient(listOf(Color.White, Color.White)),
        shape = RectangleShape,
        alpha = 0.5f
    )
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
    ) {
        VisyncPlayerTopBar(
            selectedVideofile = selectedVideofile,
            currentVideoDuration = playbackState.currentVideoDuration,
            currentVideoPosition = playbackState.currentPosition,
            playbackSpeed = playbackState.playbackSpeed,
            repeatMode = playbackState.repeatMode,
            setPlaybackSpeed = playbackControls::setPlaybackSpeed,
            toggleRepeatMode = playbackControls::toggleRepeatMode,
            seekTo = { playbackControls.seekTo(it) },
            closePlayer = closePlayer,
            onAnyInteraction = onOverlayClicked,
            modifier = backgroundModifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOverlayClicked
                )
        )
        Column(modifier = backgroundModifier) {
            VideoProgressSliderWrapper(
                useAnimatedSlider = true,
                adjustAnimation = false,
                currentVideoDuration = playbackState.currentVideoDuration,
                currentPosition = playbackState.currentPosition,
                currentPositionPollingInterval = playbackState.currentPositionPollingInterval,
                playbackSpeed = playbackState.playbackSpeed,
                isPlaying = playbackState.isPlaying,
                seekTo = playbackControls::seekTo,
                onSliderDragStart = disableAutoHiding,
                onSliderDragEnd = enableAutoHiding,
                modifier = Modifier.fillMaxWidth()
            )
            VisyncPlayerBottomControls(
                isVideoPlaying = playbackState.isPlaying,
                pause = {
                    if (isUserHost) {
                        messageSender.sendPauseMessage()
                    }
                    playbackControls.pause()
                },
                unpause = {
                    if (isUserHost) {
                        messageSender.sendUnpauseMessage()
                    }
                    playbackControls.unpause()
                },
                seekToPrev = {
                    if (isUserHost) {

                    }
                    playbackControls.seekToPrevious()
                },
                seekToNext = {
                    if (isUserHost) {

                    }
                    playbackControls.seekToNext()
                },
                onAnyInteraction = onOverlayClicked,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}