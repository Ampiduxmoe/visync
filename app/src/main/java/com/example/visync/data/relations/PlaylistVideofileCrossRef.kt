package com.example.visync.data.relations

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["playlistId", "videofileId", "position"])
data class PlaylistVideofileCrossRef(

    @ColumnInfo(index = true)
    val playlistId: Long,

    @ColumnInfo(index = true)
    val videofileId: Long,

    @ColumnInfo(index = true)
    val position: Long
)
