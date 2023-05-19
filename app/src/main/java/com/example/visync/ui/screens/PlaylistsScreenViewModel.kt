package com.example.visync.ui.screens

import androidx.lifecycle.ViewModel
import com.example.visync.data.playlists.Playlist
import com.example.visync.data.playlists.PlaylistsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlaylistsScreenViewModel @Inject constructor() : ViewModel() {

    @Inject lateinit var playlistsRepository: PlaylistsRepository

    val playlists: List<Playlist> = playlistsRepository.getAllPlaylists()
}