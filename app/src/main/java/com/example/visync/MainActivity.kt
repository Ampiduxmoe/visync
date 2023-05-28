package com.example.visync

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.visync.data.playlists.Playlist
import com.example.visync.ui.screens.PlaylistsScreen
import com.example.visync.ui.screens.PlaylistsScreenViewModel
import com.example.visync.ui.screens.RoomsScreen
import com.example.visync.ui.screens.RoomsScreenViewModel
import com.example.visync.ui.theme.VisyncTheme
import dagger.hilt.android.AndroidEntryPoint
//import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VisyncTheme {
//                val windowSize = calculateWindowSizeClass(this)

                val playlistsScreenViewModel = hiltViewModel<PlaylistsScreenViewModel>()
                val playlistsUiState by playlistsScreenViewModel
                    .uiState.collectAsStateWithLifecycle()
                var count by remember { mutableStateOf(10.toLong()) }
                Column {
                    Button(onClick = {
                        playlistsScreenViewModel.playlistsRepository.addPlaylist(
                            Playlist(count++, "another one")
                        )
                        Log.d("MyClick", "count = $count")
                    }) {
                        Text(text = "Add playlist")
                    }
                    PlaylistsScreen(
                        playlistsUiState = playlistsUiState
                    )
                }

                
//                val roomsViewModel = hiltViewModel<RoomsScreenViewModel>()
////                val roomsUiState by roomsViewModel.uiState.collectAsStateWithLifecycle()
//                RoomsScreen(
//                    roomsScreenViewModel = roomsViewModel
//                )
            }
        }
    }
}