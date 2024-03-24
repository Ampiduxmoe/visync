package com.example.visync.ui.components.player

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.visync.R
import com.example.visync.data.videofiles.Videofile

@Composable
fun VisyncPlayerTopBar(
    selectedVideofile: Videofile?,
    currentVideoDuration: Long,
    currentVideoPosition: Long,
    playbackSpeed: Float,
    repeatMode: @Player.RepeatMode Int,
    setPlaybackSpeed: (Float) -> Unit,
    toggleRepeatMode: () -> Unit,
    seekTo: (Long) -> Unit,
    closePlayer: () -> Unit,
    modifier: Modifier = Modifier,
    onAnyInteraction: () -> Unit,
) {
    Column(modifier = modifier.statusBarsPadding()) {
        Row {
            Box(modifier = Modifier.padding(8.dp)) {
                IconButton(onClick = closePlayer) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.desc_back),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            if (selectedVideofile == null) {
                Text("Can't play anything right now.")
                Text("There is no selected videofile!")
                return
            }
            val vidMetadata = selectedVideofile.metadata
            Column(
                modifier = Modifier.padding(end = 8.dp, top = 16.dp, bottom = 16.dp)
            ) {
                Text(
                    text = vidMetadata.filename,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
fun VisyncPlayerTopBarDebug(
    selectedVideofile: Videofile?,
    currentVideoDuration: Long,
    currentVideoPosition: Long,
    playbackSpeed: Float,
    repeatMode: @Player.RepeatMode Int,
    setPlaybackSpeed: (Float) -> Unit,
    toggleRepeatMode: () -> Unit,
    seekTo: (Long) -> Unit,
    closePlayer: () -> Unit,
    modifier: Modifier = Modifier,
    onAnyInteraction: () -> Unit,
) {
    Column(modifier = modifier) {
        IconButton(onClick = closePlayer) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(R.string.desc_back)
            )
        }
        Text("hello im a player")
        if (selectedVideofile == null) {
            Text("can't play anything right now")
            Text("there is no selectedVideofile")
            return
        }
        if (selectedVideofile.uri == Uri.EMPTY) {
            Text("you selected dummy videofile (${selectedVideofile.metadata.filename})")
        }
        val vidMetadata = selectedVideofile.metadata
        Text("now playing ${vidMetadata.filename}")
        Text("resolution: ${vidMetadata.width}x${vidMetadata.height}")
        Text("current speed = $playbackSpeed")
        Text("0.2x speed", modifier = Modifier.clickable { setPlaybackSpeed(0.2f)
            onAnyInteraction() })
        Text("1.0x speed", modifier = Modifier.clickable { setPlaybackSpeed(1f)
            onAnyInteraction() })
        Text("2.0x speed", modifier = Modifier.clickable { setPlaybackSpeed(2.0f)
            onAnyInteraction() })
        Text("repeat mode = $repeatMode")
        Text("toggle repeat mode", modifier = Modifier.clickable { toggleRepeatMode()
            onAnyInteraction() })
        Text("duration is ${currentVideoDuration/1000}")
        Text("current time is ${currentVideoPosition/1000}")
        Text(
            "to ${(currentVideoPosition-5000)/1000}",
            modifier = Modifier.clickable {
                seekTo(currentVideoPosition-5000)
                onAnyInteraction()
            }
        )
        Text(
            "to ${(currentVideoPosition+5000)/1000}",
            modifier = Modifier.clickable {
                seekTo(currentVideoPosition+5000)
                onAnyInteraction()
            }
        )
    }
}