package com.example.visync.data.videofiles

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.visync.converters.UriConverter
import com.example.visync.converters.VideoMetadataConverter
import com.example.visync.metadata.VideoMetadata

@Entity
data class Videofile(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(index = true)
    val videofileId: Long,

    /** `URI of type **'file:'** */
    @ColumnInfo(index = true)
    val uri: Uri,

    @ColumnInfo(index = true)
    val filepath: String,

    @TypeConverters(VideoMetadataConverter::class)
    val metadata: VideoMetadata,
) {
    fun equalsByUri(other: Videofile): Boolean = uri == other.uri
    fun equalsByFilepath(other: Videofile): Boolean = filepath == other.filepath
    fun equalsByMetadata(other: Videofile): Boolean = metadata == other.metadata
}