package com.example.visync.data.videofiles

import kotlinx.coroutines.flow.StateFlow

interface VideofilesRepository {

    val videofiles: StateFlow<List<Videofile>>

    fun getVideofile(id: Long): Videofile?

    fun tryAddVideofile(videofile: Videofile): Boolean

    fun tryAddVideofiles(videofiles: List<Videofile>): Int

    fun removeAllVideofiles()
}