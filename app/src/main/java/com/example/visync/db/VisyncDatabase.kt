package com.example.visync.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.visync.converters.UriConverter
import com.example.visync.converters.VideoMetadataConverter
import com.example.visync.data.playlists.Playlist
import com.example.visync.data.playlists.PlaylistDao
import com.example.visync.data.relations.PlaylistVideofileCrossRef
import com.example.visync.data.videofiles.Videofile
import com.example.visync.data.videofiles.VideofileDao

@Database(
    entities = [
        Playlist::class,
        Videofile::class,
        PlaylistVideofileCrossRef::class,
    ],

    version = 2
)
@TypeConverters(UriConverter::class, VideoMetadataConverter::class)
abstract class VisyncDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun videofileDao(): VideofileDao
}