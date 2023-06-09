package com.example.visync.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.visync.R

@Composable
fun VisyncPlayer(
    visyncPlayerUiState: PlayerScreenUiState,
    closePlayer: () -> Unit,
    player: @Composable () -> Unit,
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
        player()
    }
}