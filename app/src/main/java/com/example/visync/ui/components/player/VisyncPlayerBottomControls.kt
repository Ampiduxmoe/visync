package com.example.visync.ui.components.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.visync.R

@Composable
fun VisyncPlayerBottomControls(
    isVideoPlaying: Boolean,
    pause: () -> Unit,
    unpause: () -> Unit,
    seekToPrev: () -> Unit,
    seekToNext: () -> Unit,
    modifier: Modifier = Modifier,
    onAnyInteraction: () -> Unit = {},
) {
    val togglePlayback = remember(isVideoPlaying) {
        when (isVideoPlaying) {
            true -> pause
            false -> unpause
        }
    }
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onAnyInteraction
        )
    ) {
        Spacer(modifier = Modifier.weight(1f))
        SeekToPrevIconButton(
            onClick = {
                seekToPrev()
                onAnyInteraction()
            },
            modifier = Modifier.weight(1f)
        )
        PauseUnpauseIconButton(
            isVideoPlaying = isVideoPlaying,
            onClick = {
                togglePlayback()
                onAnyInteraction()
            },
            modifier = Modifier.weight(1f)
        )
        SeekToNextIconButton(
            onClick = {
                seekToNext()
                onAnyInteraction()
            },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun SeekToPrevIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        modifier = modifier,
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_seek_prev),
            contentDescription = stringResource(R.string.desc_seek_prev),
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
fun PauseUnpauseIconButton(
    isVideoPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val togglePlaybackIconId = when (isVideoPlaying) {
        true -> R.drawable.ic_pause
        false -> R.drawable.ic_play_arrow
    }
    val togglePlaybackDescriptionId = when (isVideoPlaying) {
        true -> R.string.desc_pause
        false -> R.string.desc_unpause
    }
    IconButton(
        modifier = modifier,
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(togglePlaybackIconId),
            contentDescription = stringResource(togglePlaybackDescriptionId),
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
fun SeekToNextIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        modifier = modifier,
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_seek_next),
            contentDescription = stringResource(R.string.desc_seek_next),
            modifier = Modifier.size(32.dp),
        )
    }
}

@Preview
@Composable
fun VisyncPlayerBottomControlsPreview() {
    VisyncPlayerBottomControls(
        isVideoPlaying = false,
        pause = {},
        unpause = {},
        seekToPrev = {},
        seekToNext = {},
        modifier = Modifier,
        onAnyInteraction = {},
    )
}