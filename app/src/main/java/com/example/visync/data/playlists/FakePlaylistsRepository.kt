package com.example.visync.data.playlists

import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale

class FakePlaylistsRepository : PlaylistsRepository {
    private val indexWords: List<String> = listOf(
        "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth"
    )
    private val playlists: MutableList<Playlist> = (1..9).map {
        Playlist(
            id = it.toLong(),
            name = "${indexWords[it-1].capitalize(Locale.current)} playlist"
        )
    }.toMutableList()

    override fun getAllPlaylists(): List<Playlist> {
        return playlists
    }

    override fun getPlaylist(id: Long): Playlist? {
        return playlists.firstOrNull() { it.id == id }
    }

    override fun addPlaylist(playlist: Playlist): Boolean {
        TODO("Not yet implemented")
    }
}