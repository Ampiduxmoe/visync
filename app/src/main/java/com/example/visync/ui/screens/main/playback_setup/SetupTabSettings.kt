package com.example.visync.ui.screens.main.playback_setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import kotlin.math.roundToInt

@Composable
fun SetupTabSettings(
    playbackOptions: PlaybackOptions,
    hostActions: PlaybackSetupHostActions
) {
    Column {
        Row {
            val repeatModeString = when (playbackOptions.repeatMode) {
                Player.REPEAT_MODE_OFF -> "REPEAT_MODE_OFF"
                Player.REPEAT_MODE_ONE -> "REPEAT_MODE_ONE"
                Player.REPEAT_MODE_ALL -> "REPEAT_MODE_ALL"
                else -> "UNKNOWN_REPEAT_MODE_WTF"
            }
            Text(
                text = repeatModeString,
                modifier = Modifier.clickable {
                    val newRepeatMode = (playbackOptions.repeatMode + 1) % 3
                    hostActions.setRepeatMode(newRepeatMode)
                }
            )
        }
        var localSliderValue by remember { mutableFloatStateOf(0f) }
        var isUserDraggingSlider by remember { mutableStateOf(false) }
        val valueToShow = (when (isUserDraggingSlider) {
            true -> localSliderValue
            false -> playbackOptions.playbackSpeed
        } * 10).roundToInt() / 10f
        Text("playback speed = ${valueToShow}x")
        Slider(
            value = valueToShow,
            valueRange = 0.5f..2f,
            steps = 14,
            onValueChange = {
                isUserDraggingSlider = true
                localSliderValue = it
            },
            onValueChangeFinished = {
                hostActions.setPlaybackSpeed(localSliderValue)
                isUserDraggingSlider = false
            }
        )
    }
}

@Composable
fun SetupTabSettings(
    playbackOptions: PlaybackOptions,
) {
    Column {
        Row {
            val repeatModeString = when (playbackOptions.repeatMode) {
                Player.REPEAT_MODE_OFF -> "REPEAT_MODE_OFF"
                Player.REPEAT_MODE_ONE -> "REPEAT_MODE_ONE"
                Player.REPEAT_MODE_ALL -> "REPEAT_MODE_ALL"
                else -> "UNKNOWN_REPEAT_MODE_WTF"
            }
            Text(
                text = repeatModeString
            )
        }
        val valueToShow = (playbackOptions.playbackSpeed * 10).roundToInt() / 10f
        Text("playback speed = ${valueToShow}x")
        Slider(
            value = valueToShow,
            enabled = false,
            valueRange = 0.5f..2f,
            steps = 14,
            onValueChange = {}
        )
    }
}