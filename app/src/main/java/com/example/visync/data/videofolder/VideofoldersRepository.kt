package com.example.visync.data.videofolder

import kotlinx.coroutines.flow.StateFlow

interface VideofoldersRepository {

    val videofolders: StateFlow<List<Videofolder>>

    fun getVideofolder(id: Long): Videofolder?

    fun tryAddVideofolder(videofolder: Videofolder): Boolean

    fun tryAddVideofolders(videofolders: List<Videofolder>): Int

    fun removeAllVideofolders()
}