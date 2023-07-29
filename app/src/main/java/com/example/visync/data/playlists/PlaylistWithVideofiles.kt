package com.example.visync.data.playlists

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.visync.data.relations.PlaylistVideofileCrossRef
import com.example.visync.data.videofiles.Videofile

data class PlaylistWithVideofiles(

    @Embedded
    val playlist: Playlist,

    @Relation(
        parentColumn = "playlistId",
        entityColumn = "videofileId",
        associateBy = Junction(PlaylistVideofileCrossRef::class)
    )
    val videofiles: List<Videofile>
)
