package com.example.visync.ui.screens.player

import androidx.lifecycle.ViewModel
import com.example.visync.connections.ConnectionRequest
import com.example.visync.connections.RunningConnection
import com.example.visync.connections.VisyncAdvertiserListener
import com.example.visync.connections.VisyncNearbyConnectionsImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlaybackSetupViewModel @Inject constructor(
    val visyncNearbyConnections: VisyncNearbyConnectionsImpl,
) : ViewModel() {

    val connectionsAdvertiser = visyncNearbyConnections.asAdvertiser()

    private val _playbackSetupState = MutableStateFlow(
        PlaybackSetupState(
            placeholder = ""
        )
    )
    val playbackSetupState: StateFlow<PlaybackSetupState> = _playbackSetupState

    init {
        connectionsAdvertiser.setEventListener(object : VisyncAdvertiserListener {
            override fun onIsAdvertisingChanged(isAdvertising: Boolean) { }
            override fun onNewConnectionRequest(request: ConnectionRequest) {
                request.accept()
            }
            override fun onNewRunningConnection(connection: RunningConnection) { }
            override fun onNewMessage(message: String, from: RunningConnection) { }
        })
    }

    fun sendPlaybackStartMessage() {
        val runningConnections = connectionsAdvertiser.advertiserState.value.runningConnections
        for (connection in runningConnections) {
            connection.sendMessage("playback start")
        }
    }
}

data class PlaybackSetupState(
    val placeholder: String,
)