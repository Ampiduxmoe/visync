package com.example.visync.data.videofolder

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeVideofoldersRepository : VideofoldersRepository {

    private val _videofolders = MutableStateFlow<List<Videofolder>>(
        listOf()
    )

    override val videofolders: StateFlow<List<Videofolder>> = _videofolders

    override fun getVideofolder(id: Long): Videofolder? {
        return _videofolders.value.firstOrNull { it.id == id }
    }

    override fun tryAddVideofolder(videofolder: Videofolder): Boolean {
        if (_videofolders.value.contains(videofolder)) {
            return false
        }
        _videofolders.value = _videofolders.value + videofolder
        return true
    }

    override fun tryAddVideofolders(videofolders: List<Videofolder>): Int {
        var videofoldersAdded = 0
        for (videofolder in videofolders) {
            videofoldersAdded += if (tryAddVideofolder((videofolder))) 1 else 0
        }
        return videofoldersAdded
    }

    override fun removeAllVideofolders() {
        _videofolders.value = listOf()
    }

}