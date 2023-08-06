package com.example.visync.ui.screens.main.playback_setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt

@Composable
fun SetupTabSettings(
    playbackSetupOptions: PlaybackSetupOptions,
    playbackSetupOptionSetters: PlaybackSetupOptionSetters
) {
    Column {
        Row {
            Text("doStream")
            Checkbox(
                checked = playbackSetupOptions.doStream,
                onCheckedChange = { playbackSetupOptionSetters.setDoStream(it) }
            )
        }
        Row {
            Text(
                text = playbackSetupOptions.repeatMode.toString(),
                modifier = Modifier.clickable {
                    playbackSetupOptionSetters.toggleRepeatMode()
                }
            )
        }
        var localSliderValue by remember { mutableFloatStateOf(0f) }
        var isUserDraggingSlider by remember { mutableStateOf(false) }
        val valueToShow = (when (isUserDraggingSlider) {
            true -> localSliderValue
            false ->playbackSetupOptions.playbackSpeed
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
                playbackSetupOptionSetters.setPlaybackSpeed(localSliderValue)
                isUserDraggingSlider = false
            }
        )
    }
}