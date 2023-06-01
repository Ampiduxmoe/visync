package com.example.visync.data.videofiles

import android.net.Uri
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeVideofilesRepository : VideofilesRepository {
    private val indexWords: List<String> = listOf(
        "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth"
    )

    private val _videofiles = MutableStateFlow<List<Videofile>>(
        (1..9).map { playlistId ->
            var videofileCount = 0
            (1..playlistId).map {
                Videofile(
                    id = (++videofileCount).toLong(),
                    uri = Uri.EMPTY,
                    filename = "${indexWords[it-1].capitalize(Locale.current)} videofile",
                    playlistId = playlistId.toLong()
                )
            }
        }.flatten()
    )

    override val videofiles: StateFlow<List<Videofile>> = _videofiles

    override fun getVideofile(id: Long): Videofile? {
        return _videofiles.value.firstOrNull() { it.id == id }
    }

    override fun tryAddVideofile(videoFile: Videofile): Boolean {
        if (_videofiles.value.contains(videoFile)) {
            return false
        }
        _videofiles.value = _videofiles.value + videoFile
        return true
    }
}