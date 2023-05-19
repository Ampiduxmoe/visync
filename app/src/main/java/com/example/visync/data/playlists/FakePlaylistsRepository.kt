package com.example.visync.data.playlists

class FakePlaylistsRepository : PlaylistsRepository {
    private val playlists: MutableList<Playlist> = mutableListOf(
        Playlist(
            id = 1,
            name = "First playlist"
        ),
        Playlist(
            id = 2,
            name = "Second playlist"
        ),
    )

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