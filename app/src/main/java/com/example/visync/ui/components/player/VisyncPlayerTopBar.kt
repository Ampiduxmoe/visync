package com.example.visync.ui.components.player

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
            return
        }
        Text("now playing ${selectedVideofile.metadata.filename}")
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