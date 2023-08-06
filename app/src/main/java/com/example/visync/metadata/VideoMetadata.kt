package com.example.visync.metadata

import kotlinx.serialization.Serializable

@Serializable
data class VideoMetadata(
    val filename: String,
    val duration: Long,
) {
    var altFilename: String = filename
        private set
    constructor(
        filename: String,
        altFilename: String,
        duration: Long,
    ): this(filename, duration) {
        this.altFilename = altFilename
    }
}
