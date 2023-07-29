package com.example.visync.player

import androidx.media3.common.Player
import javax.inject.Inject

class HostPlayerWrapper @Inject constructor(
    private val player: Player
): DefaultPlayerWrapper(player) {
    override val playbackControls = HostPlayerWrapperPlaybackControls(player, playbackState)
}