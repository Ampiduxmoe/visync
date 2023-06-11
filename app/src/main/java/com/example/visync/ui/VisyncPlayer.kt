package com.example.visync.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.visync.R

@Composable
fun VisyncPlayer(
    visyncPlayerUiState: VisyncPlayerUiState,
    closePlayer: () -> Unit,
    player: Player,
) {
    BackHandler {
        closePlayer()
    }
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = closePlayer) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(R.string.desc_back)
            )
        }
        Text("hello im a player")
        val selectedVideofile = visyncPlayerUiState.selectedVideofile
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
        ExoPlayerComposable(
            player = player
        )
    }
}

@Composable
fun ExoPlayerComposable(
    player: Player
) {
    val lifecycle = remember {
        mutableStateOf(Lifecycle.Event.ON_CREATE)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycle.value = event
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
             when (lifecycle.value) {
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
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16 / 16f)
    )
}