package com.example.visync.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.visync.R

@Composable
fun PlayerScreen(
    playerScreenUiState: PlayerScreenUiState,
    closePlayer: () -> Unit,
) {
    val videofilesToPlay = playerScreenUiState.videofilesToMediaItems
    BackHandler {
        closePlayer()
    }
    Column {
        IconButton(onClick = closePlayer) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(R.string.desc_back)
            )
        }
        Text("hello im a player")
        Text("now playing ${videofilesToPlay.keys.first().filename}")
    }
}