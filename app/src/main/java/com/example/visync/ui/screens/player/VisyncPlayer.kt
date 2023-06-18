package com.example.visync.ui.screens.player

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import com.example.visync.player.PlayerWrapperPlaybackControls
import com.example.visync.player.PlayerWrapperPlaybackState
import com.example.visync.ui.components.player.ExoPlayerComposable
import com.example.visync.ui.components.player.VisyncPlayerOverlay
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VisyncPlayer(
    playerUiState: VisyncPlayerUiState,
    playerPlaybackState: PlayerWrapperPlaybackState,
    playerPlaybackControls: PlayerWrapperPlaybackControls,
    showOverlay: () -> Unit,
    hideOverlay: () -> Unit,
    closePlayer: () -> Unit,
    player: Player,
) {
    val selectedVideofile = playerUiState.selectedVideofile
    val isOverlayVisible = playerUiState.isOverlayVisible
    val coroutineScope = rememberCoroutineScope()
    var currentHideWithDelayJob by remember { mutableStateOf<Job?>(null) }
    val defaultDelay = 2500L
    val hideOverlayWithDelay: (Long) -> Job = { delayMillis ->
        coroutineScope.launch {
            delay(delayMillis)
            hideOverlay()
        }
    }
    val refreshHidingDelay: (Long) -> Unit = { delayMillis ->
        currentHideWithDelayJob?.cancel()
        currentHideWithDelayJob = hideOverlayWithDelay(delayMillis)
    }
    val cancelHidingJobAndHide: () -> Unit = {
        currentHideWithDelayJob?.cancel()
        currentHideWithDelayJob = null
        hideOverlay()
    }
    val toggleOverlay: () -> Unit = {
        if (isOverlayVisible) {
            cancelHidingJobAndHide()
        } else {
            showOverlay()
            refreshHidingDelay(defaultDelay)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            cancelHidingJobAndHide()
        }
    }
    BackHandler {
        closePlayer()
    }
    if (selectedVideofile != null && selectedVideofile.uri != Uri.EMPTY) {
        ExoPlayerComposable(
            player = player,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = toggleOverlay
                )
        )
    }
    AnimatedVisibility(
        visible = playerUiState.isOverlayVisible,
        enter = fadeIn(snap()),
        exit = fadeOut(snap())
    ) {
        VisyncPlayerOverlay(
            selectedVideofile = selectedVideofile,
            playbackState = playerPlaybackState,
            playbackControls = playerPlaybackControls,
            onOverlayClicked = { refreshHidingDelay(defaultDelay) },
            closePlayer = closePlayer,
            disableAutoHiding = { currentHideWithDelayJob?.cancel() },
            enableAutoHiding = { refreshHidingDelay(defaultDelay) },
            modifier = Modifier.fillMaxSize()
        )
    }
}

