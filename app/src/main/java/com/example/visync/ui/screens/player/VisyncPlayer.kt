package com.example.visync.ui.screens.player

import android.app.Activity
import android.net.Uri
import android.util.Log
import android.util.SizeF
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import com.example.visync.player.PlayerWrapperPlaybackControls
import com.example.visync.player.PlayerWrapperPlaybackState
import com.example.visync.ui.components.player.ExoPlayerComposable
import com.example.visync.ui.components.player.VisyncPlayerOverlay
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Composable
fun VisyncPlayer(
    playerUiState: VisyncPlayerUiState,
    playerPlaybackState: PlayerWrapperPlaybackState,
    playerPlaybackControls: PlayerWrapperPlaybackControls,
    videoConfiguration: VideoConfiguration,
    physicalDevice: VisyncPhysicalDevice,
    isUserHost: Boolean,
    hostMessenger: HostPlayerMessenger,
    showOverlay: () -> Unit,
    hideOverlay: () -> Unit,
    closePlayer: () -> Unit,
    player: Player,
) {
    // TODO: toggle immersive mode each time user returns to app
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
    val window = (LocalContext.current as Activity).window
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
    val autoHidingBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    DisposableEffect(Unit) {
        defaultSystemBarsBehavior = systemUiController.systemBarsBehavior
//        systemUiController.apply {
//            isSystemBarsVisible = false
//            systemBarsBehavior = autoHidingBarsBehavior
//        }
        onDispose {
//            systemUiController.apply {
//                isSystemBarsVisible = true
//                systemBarsBehavior = defaultSystemBarsBehavior
//            }
            cancelHidingJobAndHide()
        }
    }
    BackHandler {
        closePlayer()
    }
    val (deviceWidth, deviceHeight) = with(LocalDensity.current) {
        physicalDevice.run {
//            val dpWidthFromMm = mmToDp(mmDisplayWidth, density)
//            val dpHeightFromMm = mmToDp(mmDisplayHeight, density)
//            Log.d("Device DPs from MMs", "$dpWidthFromMm x $dpHeightFromMm (density: $density)")
            pxDisplayWidth.toDp() to pxDisplayHeight.toDp()
        }
    }

    val (videoWidth, videoHeight) = with(LocalDensity.current) {
        videoConfiguration.run {
            physicalDevice.run {
                mmToDp(mmVideoWidth, density) to mmToDp(mmVideoHeight, density)
            }
        }
    }

    val (videoOffsetX, videoOffsetY) = with(LocalDensity.current) {
        videoConfiguration.run {
            physicalDevice.run {
                mmToDp(-mmDevicePositionX, density) to mmToDp(-mmDevicePositionY, density)
            }
        }
    }

    if (selectedVideofile != null && selectedVideofile.uri != Uri.EMPTY) {
        Box(modifier = Modifier.fillMaxSize()) {
            var finalOffsetX = videoOffsetX
            if (videoWidth > deviceWidth) { // if video doesn't fit it is centered (why?)
                finalOffsetX = (videoWidth - deviceWidth) / 2 + videoOffsetX
            }
            var finalOffsetY = videoOffsetY
            if (videoHeight > deviceHeight) { // if video doesn't fit it is centered (why?)
                finalOffsetY = (videoHeight - deviceHeight) / 2 + videoOffsetY
            }
//            Log.d("tag", "offsetX = $finalOffsetX, offsetY = $finalOffsetY")
//            Log.d("tag", "videoWidth = $videoWidth, videoHeight = $videoHeight")
//            Log.d("tag", "deviceWidth = $deviceWidth, deviceHeight = $deviceHeight")
            ExoPlayerComposable(
                player = player,
                modifier = Modifier
                    .absoluteOffset(
                        x = finalOffsetX,
                        y = finalOffsetY
                    )
                    .requiredSize(
                        width = videoWidth,
                        height = videoHeight
                    )
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
            hostMessenger = hostMessenger,
            onOverlayClicked = { refreshHidingDelay(defaultDelay) },
            closePlayer = closePlayer,
            disableAutoHiding = { currentHideWithDelayJob?.cancel() },
            enableAutoHiding = { refreshHidingDelay(defaultDelay) },
            modifier = Modifier.fillMaxSize()
        )
    }
}
@Serializable
data class VisyncPhysicalDevice(
    val inDisplaySize: Float,
    val pxDisplayWidth: Float,
    val pxDisplayHeight: Float,
    val mmDeviceWidth: Float,
    val mmDeviceHeight: Float,
) {
    private val wh
        get() = getWidthAndHeightMm(inDisplaySize, pxDisplayWidth, pxDisplayHeight)
    val mmDisplayWidth
        get() = wh.width
    val mmDisplayHeight
        get() = wh.height

    fun mmToDp(
        mm: Float,
        localDensityDp: Float
    ): Dp {
        return (mm * pxDisplayHeight / (mmDisplayHeight * localDensityDp)).dp
    }

    companion object {
        val NoDimensionsDevice = VisyncPhysicalDevice(
            inDisplaySize = 0f,
            mmDeviceWidth = 0f,
            mmDeviceHeight = 0f,
            pxDisplayWidth = 0f,
            pxDisplayHeight = 0f,
        )

        fun getWidthAndHeightMm(
            inDisplaySize: Float,
            pxDisplayWidth: Float,
            pxDisplayHeight: Float,
        ): SizeF {
            val d = inDisplaySize
            val w = pxDisplayWidth
            val h = pxDisplayHeight
            if (w * w + h * h <= 0f) { return SizeF(0f, 0f) }
            val a = d / sqrt(w * w + h * h)
            return SizeF(w * a * 25.4f, h * a * 25.4f)
        }
    }
}

data class VideoConfiguration(
    val mmVideoWidth: Float,
    val mmVideoHeight: Float,
    val mmDevicePositionX: Float,
    val mmDevicePositionY: Float,
)