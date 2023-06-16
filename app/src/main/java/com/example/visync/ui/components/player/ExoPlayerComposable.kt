package com.example.visync.ui.components.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView

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
                it.useController = false
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