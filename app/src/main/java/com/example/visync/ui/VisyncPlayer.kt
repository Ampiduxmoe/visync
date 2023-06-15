package com.example.visync.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.visync.R
import com.example.visync.data.videofiles.Videofile
import kotlinx.coroutines.launch

@Composable
fun VisyncPlayer(
    playerUiState: VisyncPlayerUiState,
    playerPlaybackState: VisyncPlayerPlaybackState,
    playerPlaybackControls: VisyncPlayerPlaybackControls,
    closePlayer: () -> Unit,
    player: Player,
) {
    val selectedVideofile = playerUiState.selectedVideofile
    BackHandler {
        closePlayer()
    }
    if (selectedVideofile != null && selectedVideofile.uri != Uri.EMPTY) {
        ExoPlayerComposable(
            player = player,
            modifier = Modifier.fillMaxSize()
        )
    }
    VisyncPlayerOverlay(
        selectedVideofile = selectedVideofile,
        playbackState = playerPlaybackState,
        playbackControls = playerPlaybackControls,
        closePlayer = closePlayer,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun VisyncPlayerOverlay(
    selectedVideofile: Videofile?,
    playbackState: VisyncPlayerPlaybackState,
    playbackControls: VisyncPlayerPlaybackControls,
    closePlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.background(Brush.linearGradient(listOf(Color.White, Color.White)), shape = RectangleShape, alpha = 0.5f)
    ) {
        IconButton(onClick = closePlayer) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(R.string.desc_back)
            )
        }
        Text("hello im a player")
        if (selectedVideofile == null) {
            Text("can't play anything right now")
            Text("there is no selectedVideofile")
            return
        }
        if (selectedVideofile.uri == Uri.EMPTY) {
            Text("you selected dummy videofile (${selectedVideofile.filename})")
            return
        }
        Text("now playing ${selectedVideofile.filename}")
        Text("prev", modifier = Modifier.clickable { playbackControls.seekToPrevious() })
        Text("pause", modifier = Modifier.clickable { playbackControls.pause() })
        Text("unpause", modifier = Modifier.clickable { playbackControls.unpause() })
        Text("next", modifier = Modifier.clickable { playbackControls.seekToNext() })
        Text("current speed = ${playbackState.playbackSpeed}")
        Text("0.2x speed", modifier = Modifier.clickable { playbackControls.setPlaybackSpeed(0.2f) })
        Text("1.0x speed", modifier = Modifier.clickable { playbackControls.setPlaybackSpeed(1f) })
        Text("2.0x speed", modifier = Modifier.clickable { playbackControls.setPlaybackSpeed(2.0f) })
        Text("repeat mode = ${playbackState.repeatMode}")
        Text("toggle repeat mode", modifier = Modifier.clickable { playbackControls.toggleRepeatMode() })
        Text("duration is ${playbackState.currentVideoDuration/1000}")
        Text("current time is ${playbackState.currentPosition/1000}")
        Text(
            "to ${(playbackState.currentPosition-5000)/1000}",
            modifier = Modifier.clickable {
                playbackControls.seekTo(playbackState.currentPosition-5000)
            }
        )
        Text(
            "to ${(playbackState.currentPosition+5000)/1000}",
            modifier = Modifier.clickable {
                playbackControls.seekTo(playbackState.currentPosition+5000)
            }
        )
        val useAnimatedSlider = false
        if (useAnimatedSlider) {
            AnimatedVideoProgressSlider(
                currentVideoDuration = playbackState.currentVideoDuration,
                currentPosition = playbackState.currentPosition,
                currentPositionPollingInterval = playbackState.currentPositionPollingInterval,
                playbackSpeed = playbackState.playbackSpeed,
                isPlaying = playbackState.isPlaying,
                seekTo = playbackControls::seekTo,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            VideoProgressSlider(
                currentVideoDuration = playbackState.currentVideoDuration,
                currentPosition = playbackState.currentPosition,
                seekTo = playbackControls::seekTo
            )
        }
    }
}

@Composable
fun VideoProgressSlider(
    currentVideoDuration: Long,
    currentPosition: Long,
    seekTo: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var canPlayerChangeSliderValue by remember { mutableStateOf(true) }
    LaunchedEffect(currentPosition, currentVideoDuration) {
        if (!canPlayerChangeSliderValue) {
            return@LaunchedEffect
        }
        if (currentVideoDuration == 0L) {
            sliderValue = 0f
            return@LaunchedEffect
        }
        val newSliderValue = currentPosition / currentVideoDuration.toFloat()
        sliderValue = newSliderValue
    }
    Slider(
        value = sliderValue,
        onValueChange = {
            canPlayerChangeSliderValue = false
            sliderValue = it
            seekTo(it)
        },
        onValueChangeFinished = {
            canPlayerChangeSliderValue = true
        },
        valueRange = 0f..1f,
        modifier = modifier
    )
}

/**
 *  Slider that smoothly animates its value to the next position
 *  when [currentPosition] or [isPlaying] changes with respect to
 *  current [playbackSpeed] and [currentPositionPollingInterval].
 *
 *  It is not recommended to use this slider if there are frequent
 *  and/or drastic changes to [playbackSpeed] with actual player
 *  playback speed being not properly synchronized to it, slider
 *  will jitter because of this.
 *
 *  For playback speed possibly being out of sync with actual playback
 *  see [this issue](https://github.com/google/ExoPlayer/issues/7982).
 */
@Composable
fun AnimatedVideoProgressSlider(
    currentVideoDuration: Long,
    currentPosition: Long,
    currentPositionPollingInterval: Int,
    playbackSpeed: Float,
    isPlaying: Boolean,
    seekTo: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sliderValue = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val animationDurationMultiplier = 1.25f
    var canPlayerChangeSliderValue by remember { mutableStateOf(true) }
    val oneSecondProgressIncrement = remember(currentVideoDuration, playbackSpeed) {
        when (currentVideoDuration) {
            0L -> 0f
            else -> 1000f / currentVideoDuration * playbackSpeed
        }
    }
    LaunchedEffect(currentPosition, currentVideoDuration) {
        if (!canPlayerChangeSliderValue) {
            return@LaunchedEffect
        }
        if (currentVideoDuration == 0L) {
            sliderValue.snapTo(0f)
            return@LaunchedEffect
        }
        val newSliderValue = currentPosition / currentVideoDuration.toFloat()
        sliderValue.snapTo(newSliderValue)
        if (!isPlaying) {
            sliderValue.snapTo(newSliderValue)
            return@LaunchedEffect
        }
        val pollingIntervalMultiplier = currentPositionPollingInterval / 1000f
        val sliderIncrement = (
            oneSecondProgressIncrement *
            pollingIntervalMultiplier *
            animationDurationMultiplier
        )
        val animationDuration = currentPositionPollingInterval * animationDurationMultiplier
        sliderValue.animateTo(
            targetValue = newSliderValue + sliderIncrement,
            animationSpec = tween(
                durationMillis = animationDuration.toInt(),
                easing = LinearEasing
            )
        )
    }
    LaunchedEffect(isPlaying, playbackSpeed) {
        if (!canPlayerChangeSliderValue) {
            return@LaunchedEffect
        }
        if (!isPlaying) {
            sliderValue.stop()
            return@LaunchedEffect
        }
        if (currentVideoDuration == 0L) {
            return@LaunchedEffect
        }
        val pollingIntervalMultiplier = currentPositionPollingInterval / 1000f
        val sliderIncrement = (
            oneSecondProgressIncrement *
            pollingIntervalMultiplier *
            animationDurationMultiplier
        )
        val animationDuration = currentPositionPollingInterval * animationDurationMultiplier
        sliderValue.animateTo(
            targetValue = sliderValue.value + sliderIncrement,
            animationSpec = tween(
                durationMillis = animationDuration.toInt(),
                easing = LinearEasing
            )
        )
    }
    Slider(
        value = sliderValue.value,
        onValueChange = {
            canPlayerChangeSliderValue = false
            coroutineScope.launch {
                sliderValue.snapTo(it)
            }
            seekTo(it)
        },
        onValueChangeFinished = {
            canPlayerChangeSliderValue = true
        },
        valueRange = 0f..1f,
        modifier = modifier
    )
}

@Composable
fun ExoPlayerComposable(
    player: Player,
    modifier: Modifier = Modifier,
) {
    val lifecycleState = remember {
        mutableStateOf(Lifecycle.Event.ON_CREATE)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycleState.value = event
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    AndroidView(
        factory = { context ->
            PlayerView(context).also {
                it.player = player
            }
        },
        update = {
             when (lifecycleState.value) {
                 Lifecycle.Event.ON_STOP -> {
                     it.onPause()
                     it.player?.stop()
                 }
                 Lifecycle.Event.ON_RESUME -> {
                     it.onResume()
                     it.player?.prepare()
                 }
                 else -> Unit
             }
        },
        modifier = modifier
    )
}