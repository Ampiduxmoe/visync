package com.example.visync.data.playlists

import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakePlaylistsRepository : PlaylistsRepository {

    private val indexWords: List<String> = listOf(
        "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth"
    )

    private val _playlists = MutableStateFlow<List<Playlist>>(
        (1..9).map {
            Playlist(
                id = it.toLong(),
                name = "${indexWords[it-1].capitalize(Locale.current)} playlist"
            )
        }
    )

    override val playlists: StateFlow<List<Playlist>> = _playlists

    override fun getPlaylist(id: Long): Playlist? {
        return _playlists.value.firstOrNull() { it.id == id }
    }

    override fun addPlaylist(playlist: Playlist): Boolean {
        if (_playlists.value.contains(playlist)) {
            return false
        }
        _playlists.value = _playlists.value + playlist
        return true
    }
}