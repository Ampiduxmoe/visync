package com.example.visync.data.playlists

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Playlist(

    @PrimaryKey(autoGenerate = true)
    val playlistId: Long,

    val name: String,
)