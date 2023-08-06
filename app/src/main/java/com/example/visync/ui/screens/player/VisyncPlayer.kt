package com.example.visync.ui.screens.player

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import com.example.visync.player.PlayerWrapperPlaybackControls
import com.example.visync.player.PlayerWrapperPlaybackState
import com.example.visync.ui.PlayerMessageSender
import com.example.visync.ui.components.player.ExoPlayerComposable
import com.example.visync.ui.components.player.VisyncPlayerOverlay
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VisyncPlayer(
    playerUiState: VisyncPlayerUiState,
    playerPlaybackState: PlayerWrapperPlaybackState,
    playerPlaybackControls: PlayerWrapperPlaybackControls,
    isUserHost: Boolean,
    messageSender: PlayerMessageSender,
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
    val systemUiController = rememberSystemUiController()
    var defaultSystemBarsBehavior by remember { mutableIntStateOf(-1) }
    val autoHidingBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    DisposableEffect(Unit) {
        defaultSystemBarsBehavior = systemUiController.systemBarsBehavior
        systemUiController.apply {
            isSystemBarsVisible = false
            systemBarsBehavior = autoHidingBarsBehavior
        }
        onDispose {
            systemUiController.apply {
                isSystemBarsVisible = true
                systemBarsBehavior = defaultSystemBarsBehavior
            }
            cancelHidingJobAndHide()
        }
    }
    BackHandler {
        closePlayer()
    }
    val device = when (isUserHost) {
        true -> ZB631KL
        false -> XiaomiRedmiNote11
    }
    val videoConfiguration = when (isUserHost) {
        true -> VideoConfiguration(
            mmVideoWidth = 192f,
            mmVideoHeight = 108f,
            mmDevicePositionX = 100f,
            mmDevicePositionY = 10f,
        )
        false -> VideoConfiguration(
            mmVideoWidth = 192f,
            mmVideoHeight = 108f,
            mmDevicePositionX = 15f,
            mmDevicePositionY = 10f,
        )
    }
    val (videoWidth, videoHeight) = with(LocalDensity.current) {
        videoConfiguration.run {
            device.run {
                mmToDp(mmVideoWidth, density) to mmToDp(mmVideoHeight, density)
            }
        }
    }
    val (videoOffsetX, videoOffsetY) = with(LocalDensity.current) {
        videoConfiguration.run {
            device.run {
                -mmToDp(mmDevicePositionX, density) to mmToDp(mmDevicePositionY, density)
            }
        }
    }

    val (deviceWidth, deviceHeight) = with(LocalDensity.current) {
        device.run {
            pxDisplayWidth.toDp() to pxDisplayHeight.toDp()
        }
    }
    if (selectedVideofile != null && selectedVideofile.uri != Uri.EMPTY) {
        Box(modifier = Modifier.fillMaxSize()) {
            ExoPlayerComposable(
                player = player,
                modifier = Modifier
                    .absoluteOffset(
                        x = (videoWidth - deviceWidth) / 2 + videoOffsetX,
                        y = videoOffsetY
                    )
                    .requiredSize(width = videoWidth, height = videoHeight)
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = toggleOverlay
            )
    )
    AnimatedVisibility(
        visible = playerUiState.isOverlayVisible,
        enter = fadeIn(snap()),
        exit = fadeOut(snap())
    ) {
        VisyncPlayerOverlay(
            selectedVideofile = selectedVideofile,
            playbackState = playerPlaybackState,
            playbackControls = playerPlaybackControls,
            isUserHost = isUserHost,
            messageSender = messageSender,
            onOverlayClicked = { refreshHidingDelay(defaultDelay) },
            closePlayer = closePlayer,
            disableAutoHiding = { currentHideWithDelayJob?.cancel() },
            enableAutoHiding = { refreshHidingDelay(defaultDelay) },
            modifier = Modifier.fillMaxSize()
        )
    }
}
data class VisyncPhysicalDevice(
    val mmDeviceWidth: Float,
    val mmDeviceHeight: Float,
    val mmDisplayWidth: Float,
    val mmDisplayHeight: Float,
    val pxDisplayWidth: Float,
    val pxDisplayHeight: Float,
) {
    fun mmToDp(
        mm: Float,
        localDensityDp: Float
    ): Dp {
        return (mm * pxDisplayHeight / (mmDisplayHeight * localDensityDp)).dp
    }
}

data class VideoConfiguration(
    val mmVideoWidth: Float,
    val mmVideoHeight: Float,
    val mmDevicePositionX: Float,
    val mmDevicePositionY: Float,
)

val ZB631KL = VisyncPhysicalDevice(
    mmDeviceWidth = 75.50f,
    mmDeviceHeight = 157.90f,
    mmDisplayWidth = 68.50f,
    mmDisplayHeight = 144.61f,
    pxDisplayWidth = 1080f,
    pxDisplayHeight = 2280f,
)

val XiaomiRedmiNote11 = VisyncPhysicalDevice(
    mmDeviceWidth = 73.87f,
    mmDeviceHeight = 158.87f,
    mmDisplayWidth = 67.02f,
    mmDisplayHeight = 148.93f,
    pxDisplayWidth = 1080f,
    pxDisplayHeight = 2400f,
)
