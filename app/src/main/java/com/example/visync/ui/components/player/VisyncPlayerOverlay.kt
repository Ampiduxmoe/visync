package com.example.visync.ui.components.player

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.visync.data.videofiles.Videofile
import com.example.visync.player.DefaultPlayerWrapperPlaybackControls
import com.example.visync.player.PlayerWrapperPlaybackControls
import com.example.visync.player.PlayerWrapperPlaybackState
import com.example.visync.ui.screens.main.playback_setup.EndpointPingData
import com.example.visync.ui.screens.main.playback_setup.getFakeVideofiles
import com.example.visync.ui.screens.player.HostPlayerMessenger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VisyncPlayerOverlay(
    selectedVideofile: Videofile?,
    playbackState: PlayerWrapperPlaybackState,
    playbackControls: PlayerWrapperPlaybackControls,
    isUserHost: Boolean,
    hostMessenger: HostPlayerMessenger,
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
            val coroutineScope = rememberCoroutineScope()
            VideoProgressSliderWrapper(
                useAnimatedSlider = false,
                adjustAnimation = false,
                currentVideoDuration = playbackState.currentVideoDuration,
                currentPosition = playbackState.currentPosition,
                currentPositionPollingInterval = playbackState.currentPositionPollingInterval,
                playbackSpeed = playbackState.playbackSpeed,
                isPlaying = playbackState.isPlaying,
                seekTo = { seekTo ->
                    if (isUserHost) {
                        val videoDuration = playbackState.currentVideoDuration
                        val timeMillis = (videoDuration * seekTo).toLong()
                        hostMessenger.sendSeekTo(timeMillis)
                        val pingData = hostMessenger.getPingData()
                        val guestsCount = pingData.size
                        val actionDelay = when {
                            guestsCount > 0 -> pingData.maxOf {
                                it.pingData.weightedAverage
                            }.toLong()
                            else -> 0L
                        }
                        coroutineScope.launch {
                            delay(actionDelay)
                            playbackControls.seekTo(seekTo)
                        }
                        return@VideoProgressSliderWrapper
                    }
                    playbackControls.seekTo(seekTo)
                },
                onSliderDragStart = disableAutoHiding,
                onSliderDragEnd = enableAutoHiding,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
            )
            VisyncPlayerBottomControls(
                isVideoPlaying = playbackState.isPlaying,
                pause = {
                    if (isUserHost) {
                        hostMessenger.sendPause()
                        val pingData = hostMessenger.getPingData()
                        val guestsCount = pingData.size
                        val actionDelay = when {
                            guestsCount > 0 -> pingData.maxOf {
                                it.pingData.weightedAverage
                            }.toLong()
                            else -> 0L
                        }
                        // actually we don't want any delay since all watchers seek to our pause position
                        playbackControls.pause()
                        return@VisyncPlayerBottomControls
                    }
                    playbackControls.pause()
                },
                unpause = {
                    if (isUserHost) {
                        hostMessenger.sendUnpause()
                        val pingData = hostMessenger.getPingData()
                        val guestsCount = pingData.size
                        val actionDelay = when {
                            guestsCount > 0 -> pingData.maxOf {
                                Log.d("avg", "${it.pingData.weightedAverage}")
                                it.pingData.weightedAverage
                            }.toLong()
                            else -> 0L
                        }
                        coroutineScope.launch {
                            delay(actionDelay)
                            playbackControls.unpause()
                        }
                        return@VisyncPlayerBottomControls
                    }
                    playbackControls.unpause()
                },
                seekToPrev = {
                    if (isUserHost) {
                        // TODO
                    }
                    playbackControls.seekToPrevious()
                },
                seekToNext = {
                    if (isUserHost) {
                        // TODO
                    }
                    playbackControls.seekToNext()
                },
                onAnyInteraction = onOverlayClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
@Preview(widthDp=360, heightDp=760)
fun VisyncPlayerOverlayPreview() {
    VisyncPlayerOverlay(
        selectedVideofile = getFakeVideofiles()[0],
        playbackState = getFakePlayerWrapperPlaybackState(),
        playbackControls = getFakePlayerWrapperPlaybackControls(),
        isUserHost = true,
        hostMessenger = getFakeHostPlayerMessenger(),
        onOverlayClicked = {},
        closePlayer = {},
        disableAutoHiding = {},
        enableAutoHiding = {},
        modifier = Modifier.fillMaxSize()
    )
}

private fun getFakePlayerWrapperPlaybackState(): PlayerWrapperPlaybackState {
    return PlayerWrapperPlaybackState(
        currentMediaItem = null,
        playerState = Player.STATE_IDLE,
        playWhenReady = false,
        isPlaying = false,
        playbackSpeed = 1f,
        currentVideoDuration = 0,
        currentPosition = 0,
        currentPositionPollingInterval = 125,
        repeatMode = Player.REPEAT_MODE_OFF,
        hasPrevious = false,
        hasNext = false,
        volume = 0,
        muted = false
    )
}

private fun getFakePlayerWrapperPlaybackControls(): PlayerWrapperPlaybackControls {
    return object : PlayerWrapperPlaybackControls {
        override fun seekToPrevious() {}
        override fun seekToNext() {}
        override fun pause() {}
        override fun unpause() {}
        override fun seekTo(progress: Float) {}
        override fun seekTo(timeMillis: Long) {}
        override fun setPlaybackSpeed(speed: Float) {}
        override fun setRepeatMode(repeatMode: Int) {}
        override fun toggleRepeatMode(): Int = 0
    }
}

private fun getFakeHostPlayerMessenger(): HostPlayerMessenger {
    return object : HostPlayerMessenger {
        override fun getPingData(): List<EndpointPingData> = listOf()
        override fun sendPause() {}
        override fun sendUnpause() {}
        override fun sendSeekTo(seekTo: Long) {}
    }
}