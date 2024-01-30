package com.example.visync.ui.screens.main.playback_setup

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.visync.R
import kotlin.math.roundToInt

@Composable
fun SetupTabSettings(
    playbackOptions: PlaybackOptions,
    hostActions: PlaybackSetupHostActions
) {
    SetupTabSettings(
        isUserHost = true,
        playbackOptions = playbackOptions,
        hostActions = hostActions
    )
}

@Composable
fun SetupTabSettings(
    playbackOptions: PlaybackOptions,
) {
    SetupTabSettings(
        isUserHost = false,
        playbackOptions = playbackOptions
    )
}

@Composable
private fun SetupTabSettings(
    isUserHost: Boolean,
    playbackOptions: PlaybackOptions,
    hostActions: PlaybackSetupHostActions? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        val iconBgColor = MaterialTheme.colorScheme.primary
        val iconColor = MaterialTheme.colorScheme.onPrimary
        val activeIconColor = MaterialTheme.colorScheme.tertiaryContainer // TODO: animate to it
        val iconContainerWidth = 64.dp
        val iconContainerBorderRadius = 8.dp
        val iconSize = 48.dp

        val titleStyle = MaterialTheme.typography.titleLarge
        val titleWeight = FontWeight.Normal
        PlaybackOption(
            title = {
                Text(
                    text = "Repeat mode",
                    style = titleStyle,
                    fontWeight = titleWeight,
                )
            },
            icon = {
                val defaultValue = Player.REPEAT_MODE_OFF
                val isDefaultValue = playbackOptions.repeatMode == defaultValue
                val resetToDefaultValue = { hostActions?.setRepeatMode(defaultValue) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .width(iconContainerWidth)
                        .fillMaxHeight()
                        .background(
                            color = iconBgColor,
                            shape = RoundedCornerShape(iconContainerBorderRadius)
                        )
                        .then(if (!isUserHost || isDefaultValue) Modifier else Modifier.clickable {
                            resetToDefaultValue()
                        })
                ) {
                    val isRepeatOne = playbackOptions.repeatMode == Player.REPEAT_MODE_ONE
                    Icon(
                        painter = if (isRepeatOne) painterResource(id = R.drawable.ic_repeat_one)
                            else painterResource(id = R.drawable.ic_repeat),
                        contentDescription = stringResource(id = R.string.desc_repeat_icon),
                        tint = if (isDefaultValue) iconColor else activeIconColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
            },
        ) {
            RepeatModeSwitch(
                isUserHost = isUserHost,
                repeatMode = playbackOptions.repeatMode,
                setRepeatMode = { hostActions?.setRepeatMode(it) }
            )
        }
        PlaybackOption(
            title = {
                Text(
                    text = "Playback speed",
                    style = titleStyle,
                    fontWeight = titleWeight,
                )
            },
            icon = {
                val defaultValue = 1f
                val isDefaultValue = playbackOptions.playbackSpeed == defaultValue
                val resetToDefaultValue = { hostActions?.setPlaybackSpeed(defaultValue) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .width(iconContainerWidth)
                        .fillMaxHeight()
                        .background(
                            color = iconBgColor,
                            shape = RoundedCornerShape(iconContainerBorderRadius)
                        )
                        .then(if (!isUserHost || isDefaultValue) Modifier else Modifier.clickable {
                            resetToDefaultValue()
                        })
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_fast_forward),
                        contentDescription = stringResource(id = R.string.desc_playback_speed_icon),
                        tint = if (isDefaultValue) iconColor else activeIconColor,
                        modifier = Modifier.size(iconSize)
                    )
                    val animatedSpeedValue by animateFloatAsState(
                        targetValue = playbackOptions.playbackSpeed,
                        animationSpec = tween(
                            durationMillis = 400,
                            easing = FastOutSlowInEasing
                        ),
                        label = "animatePlaybackSpeedValue"
                    )
                    val valueToShow = (animatedSpeedValue * 10).roundToInt() / 10f
                    Text(
                        text = "${valueToShow}x",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDefaultValue) iconColor else activeIconColor
                    )
                }
            },
        ) {
            PlaybackSpeedSlider(
                isUserHost = isUserHost,
                playbackSpeed = playbackOptions.playbackSpeed,
                setPlaybackSpeed = { hostActions?.setPlaybackSpeed(it) }
            )
        }
    }
}

@Composable
private fun PlaybackOption(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .height(96.dp)
            .then(modifier)
    ) {
        icon()
        Divider(
            modifier = Modifier
                .padding(8.dp)
                .width((1f / LocalDensity.current.density).dp)
                .fillMaxHeight()
                .background(DividerDefaults.color)
        )
        Column(
            verticalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxSize()
        ) {
            title()
            content()
        }
    }
}

@Composable
private fun RepeatModeSwitch(
    isUserHost: Boolean,
    repeatMode: @Player.RepeatMode Int,
    setRepeatMode: (@Player.RepeatMode Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val repeatModeString = when (repeatMode) {
        Player.REPEAT_MODE_OFF -> "Do no repeat"
        Player.REPEAT_MODE_ONE -> "Repeat one"
        Player.REPEAT_MODE_ALL -> "Repeat playlist"
        else -> "UNKNOWN_REPEAT_MODE"
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .then(modifier)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(id = R.string.desc_prev_repeat_mode),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = repeatModeString,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(128.dp)
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = stringResource(id = R.string.desc_next_repeat_mode),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Row {
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .then(if (!isUserHost) Modifier else Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val newRepeatMode = Math.floorMod(repeatMode - 1, 3)
                        setRepeatMode(newRepeatMode)
                    })
            )
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.33f)
            )
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .then(if (!isUserHost) Modifier else Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val newRepeatMode = (repeatMode + 1) % 3
                        setRepeatMode(newRepeatMode)
                    })
            )
        }
    }
}

@Composable
private fun PlaybackSpeedSlider(
    isUserHost: Boolean,
    playbackSpeed: Float,
    setPlaybackSpeed: (Float) -> Unit,
) {
    var localSliderValue by remember(playbackSpeed) { mutableFloatStateOf(playbackSpeed) }
    var isUserDraggingSlider by remember { mutableStateOf(false) }
    val valueToShow = (localSliderValue * 10).roundToInt() / 10f
    Column {
        val minValue = 0.5f
        val maxValue = 2f
        val valueRange = maxValue - minValue
        val offset = (valueToShow - minValue) / valueRange
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .graphicsLayer {
                    this.translationX += offset * this.size.width
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .wrapContentSize()
                    .graphicsLayer {
                        this.translationX -= this.size.width / 2
                        this.translationY += 4.dp.toPx()
                    },
            ) {
                Text(
                    text = "${valueToShow}",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier

                )
            }
        }
        Slider(
            value = valueToShow,
            valueRange = minValue..maxValue,
            steps = 14,
            enabled = isUserHost,
            onValueChange = if (isUserHost) ({
                isUserDraggingSlider = true
                localSliderValue = it
            }) else ({}),
            onValueChangeFinished = if (isUserHost) ({
                setPlaybackSpeed(localSliderValue)
                isUserDraggingSlider = false
            }) else ({}),
            modifier = Modifier.height(32.dp)
        )
    }
}

@Composable
@Preview(widthDp=360, heightDp=760)
private fun HostSetupTabSettingsPreview() {
    Surface(modifier = Modifier.fillMaxSize()) {
        SetupTabSettings(
            playbackOptions = getFakePlaybackOptions(),
            hostActions = getFakePlaybackSetupHostActions()
        )
    }
}

@Composable
@Preview(widthDp=360, heightDp=760)
private fun GuestSetupTabSettingsPreview() {
    Surface(modifier = Modifier.fillMaxSize()) {
        SetupTabSettings(
            playbackOptions = getFakePlaybackOptions()
        )
    }
}