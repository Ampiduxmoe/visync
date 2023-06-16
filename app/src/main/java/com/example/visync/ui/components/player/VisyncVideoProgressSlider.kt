package com.example.visync.ui.components.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun VideoProgressSliderWrapper(
    useAnimatedSlider: Boolean,
    adjustPlaybackSpeed: Boolean,
    currentVideoDuration: Long,
    currentPosition: Long,
    currentPositionPollingInterval: Int,
    playbackSpeed: Float,
    isPlaying: Boolean,
    seekTo: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onSliderDragStart: () -> Unit = {},
    onSliderDragEnd: () -> Unit = {},
) {
    if (useAnimatedSlider) {
        val finalPlaybackSpeed = when (adjustPlaybackSpeed) {
            true -> ensureActualPlaybackSpeedChange(
                currentPosition = currentPosition,
                currentPositionPollingInterval = currentPositionPollingInterval,
                newPlaybackSpeed = playbackSpeed,
                confidenceFactor = 5
            )
            false -> playbackSpeed
        }
        AnimatedVideoProgressSlider(
            currentVideoDuration = currentVideoDuration,
            currentPosition = currentPosition,
            currentPositionPollingInterval = currentPositionPollingInterval,
            playbackSpeed = finalPlaybackSpeed,
            isPlaying = isPlaying,
            seekTo = seekTo,
            modifier = modifier,
            onSliderDragStart = onSliderDragStart,
            onSliderDragEnd = onSliderDragEnd
        )
    } else {
        VideoProgressSlider(
            currentVideoDuration = currentVideoDuration,
            currentPosition = currentPosition,
            seekTo = seekTo,
            modifier = modifier,
            onSliderDragStart = onSliderDragStart,
            onSliderDragEnd = onSliderDragEnd
        )
    }
}

@Composable
fun VideoProgressSlider(
    currentVideoDuration: Long,
    currentPosition: Long,
    seekTo: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onSliderDragStart: () -> Unit = {},
    onSliderDragEnd: () -> Unit = {},
) {
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isUserDraggingSlider by remember { mutableStateOf(true) }
    LaunchedEffect(currentPosition, currentVideoDuration) {
        if (isUserDraggingSlider) {
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
            if (!isUserDraggingSlider) {
                isUserDraggingSlider = true
                onSliderDragStart()
            }
            sliderValue = it
            seekTo(it)
        },
        onValueChangeFinished = {
            isUserDraggingSlider = false
            onSliderDragEnd()
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
    onSliderDragStart: () -> Unit = {},
    onSliderDragEnd: () -> Unit = {},
) {
    val sliderValue = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val animationDurationMultiplier = 1.25f
    var isUserDraggingSlider by remember { mutableStateOf(false) }
    val oneSecondProgressIncrement = remember(currentVideoDuration, playbackSpeed) {
        when (currentVideoDuration) {
            0L -> 0f
            else -> 1000f / currentVideoDuration * playbackSpeed
        }
    }
    LaunchedEffect(currentPosition, currentVideoDuration) {
        if (isUserDraggingSlider) {
            return@LaunchedEffect
        }
        if (currentVideoDuration == 0L) {
            sliderValue.snapTo(0f)
            return@LaunchedEffect
        }
        val newSliderValue = currentPosition / currentVideoDuration.toFloat()
        sliderValue.snapTo(newSliderValue)
        if (!isPlaying) {
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
        if (isUserDraggingSlider) {
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
            if (!isUserDraggingSlider) {
                isUserDraggingSlider = true
                onSliderDragStart()
            }
            coroutineScope.launch {
                sliderValue.snapTo(it)
            }
            seekTo(it)
        },
        onValueChangeFinished = {
            isUserDraggingSlider = false
            onSliderDragEnd()
        },
        valueRange = 0f..1f,
        modifier = modifier
    )
}

/**
 *  Calculate if playback speed change really happened
 *  with respect to [currentPositionPollingInterval].
 *
 *  This is a workaround fix for [this issue](https://github.com/google/ExoPlayer/issues/7982).
 *
 *  @param currentPosition last known playback position
 *  @param currentPositionPollingInterval delay between playback position updates
 *  @param newPlaybackSpeed last known playback speed
 *  @param confidenceFactor how much closer should new speed match playback
 *  in comparison to the old speed
 *
 *  @return [newPlaybackSpeed] if it better matches
 *  [currentPosition] change after previous invocation.
 *  Otherwise returns playback speed that was remembered before.
 */
@Composable
private fun ensureActualPlaybackSpeedChange(
    currentPosition: Long,
    currentPositionPollingInterval: Int,
    newPlaybackSpeed: Float,
    confidenceFactor: Int,
): Float {
    var prevPlaybackSpeed by remember { mutableFloatStateOf(newPlaybackSpeed) }
    var prevPosition by remember { mutableLongStateOf(currentPosition) }
    if (newPlaybackSpeed == prevPlaybackSpeed) {
        prevPosition = currentPosition
        return newPlaybackSpeed
    }
    if (currentPosition == prevPosition) {
        return prevPlaybackSpeed
    }
    val prevCalculatedPositionChange = currentPositionPollingInterval * prevPlaybackSpeed
    val newCalculatedPositionChange = currentPositionPollingInterval * newPlaybackSpeed
    val actualPositionChange = currentPosition - prevPosition
    val differenceWithPrevSpeed = abs(actualPositionChange - prevCalculatedPositionChange)
    val differenceWithNewSpeed = abs(actualPositionChange - newCalculatedPositionChange)
    val speedChangeHappened = differenceWithNewSpeed * confidenceFactor < differenceWithPrevSpeed
    if (speedChangeHappened) {
        prevPosition = currentPosition
        prevPlaybackSpeed = newPlaybackSpeed
        return newPlaybackSpeed
    }
    prevPosition = currentPosition
    return prevPlaybackSpeed
}