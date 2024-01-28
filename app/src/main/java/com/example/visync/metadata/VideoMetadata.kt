package com.example.visync.metadata

import kotlinx.serialization.Serializable

@Serializable
data class VideoMetadata(
    val filename: String,
    val duration: Long,
    val width: Float,
    val height: Float,
) {
    var altFilename: String = filename
        private set
    constructor(
        filename: String,
        altFilename: String,
        duration: Long,
        width: Float,
        height: Float,
    ): this(filename, duration, width, height) {
        this.altFilename = altFilename
    }

    fun equalsByContent(other: VideoMetadata): Boolean {
        return duration == other.duration &&
            width == other.width &&
            height == other.height
    }
}
