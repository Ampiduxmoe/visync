package com.example.visync.converters

import androidx.room.TypeConverter
import com.example.visync.metadata.VideoMetadata
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class VideoMetadataConverter {
    @TypeConverter
    fun fromString(value: String?): VideoMetadata? {
        return value?.let { Json.decodeFromString(it) }
    }
    @TypeConverter
    fun toString(videoMetadata: VideoMetadata?): String? {
        return videoMetadata?.let { Json.encodeToString(it) }
    }
}