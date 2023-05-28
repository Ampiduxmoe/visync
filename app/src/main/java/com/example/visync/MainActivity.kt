package com.example.visync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.visync.ui.screens.PlaylistsScreen
import com.example.visync.ui.screens.PlaylistsScreenViewModel
import com.example.visync.ui.screens.RoomsScreen
import com.example.visync.ui.screens.RoomsScreenViewModel
import com.example.visync.ui.theme.VisyncTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VisyncTheme {
//                val playlistsScreenViewModel = hiltViewModel<PlaylistsScreenViewModel>()
//                PlaylistsScreen(
//                    playlistsScreenViewModel = playlistsScreenViewModel
//                )
                
                val roomsViewModel = hiltViewModel<RoomsScreenViewModel>()
                RoomsScreen(
                    roomsScreenViewModel = roomsViewModel
                )
            }
        }
    }
}