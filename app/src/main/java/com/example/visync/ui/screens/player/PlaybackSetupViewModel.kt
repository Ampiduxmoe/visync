package com.example.visync.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.visync.connections.ConnectionRequest
import com.example.visync.connections.RunningConnection
import com.example.visync.connections.VisyncAdvertiserListener
import com.example.visync.connections.VisyncNearbyConnectionsImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class PlaybackSetupViewModel @Inject constructor(
    val visyncNearbyConnections: VisyncNearbyConnectionsImpl,
) : ViewModel() {

    val connectionsAdvertiser = visyncNearbyConnections.asAdvertiser()

    private val _playbackSetupState = MutableStateFlow(
        PlaybackSetupState(
            canChangePlaybackSettings = false
        )
    )
    val playbackSetupState: StateFlow<PlaybackSetupState> = _playbackSetupState

    val events = object : PlaybackSetupEvents {
        override var onOpenPlayerMessage: (() -> Unit)? = null
    }

    init {
        connectionsAdvertiser.setEventListener(object : VisyncAdvertiserListener {
            override fun onIsAdvertisingChanged(isAdvertising: Boolean) { }
            override fun onNewConnectionRequest(request: ConnectionRequest) {
                request.accept()
            }
            override fun onNewRunningConnection(connection: RunningConnection) { }
            override fun onNewMessage(message: String, from: RunningConnection) {
                playbackSetupMessageSystem.process(message, from)
            }
        })

        observeAdvertiserState()
    }

    fun observeAdvertiserState() {
        viewModelScope.launch {
            connectionsAdvertiser.advertiserState.collect { advertiserState ->

            }
        }
    }

    fun allowSetupEditing() {
        _playbackSetupState.value = _playbackSetupState.value.copy(
            canChangePlaybackSettings = true
        )
    }

    fun disallowSetupEditing() {
        _playbackSetupState.value = _playbackSetupState.value.copy(
            canChangePlaybackSettings = false
        )
    }

    fun sendOpenPlayer() {
        val runningConnections = connectionsAdvertiser.advertiserState.value.runningConnections
        playbackSetupMessageSystem.sendOpenPlayer(runningConnections)
    }

    private val playbackSetupMessageSystem = object {
        val PLAYBACK_SETUP_MESSAGE_SYSTEM_VER = 1
        val jsonIgnoreUnknownKeys = Json { ignoreUnknownKeys = true }

        fun process(msg: String, sender: RunningConnection) {
            val visyncMessage = jsonIgnoreUnknownKeys.decodeFromString<VisyncMessage>(msg)
            when (visyncMessage.type) {
                VersionMessage::class.simpleName -> {
                    val versionMessage = Json.decodeFromString<VersionMessage>(msg)
                    if (versionMessage.version != PLAYBACK_SETUP_MESSAGE_SYSTEM_VER) {
                        TODO("Tell user about conflicting app versions")
                    }
                }
                OpenPlayerMessage::class.simpleName -> {
                    events.onOpenPlayerMessage?.invoke()
                }
            }
        }

        fun sendOpenPlayer(to: List<RunningConnection>) {
            val openPlayerMsg = OpenPlayerMessage()
            val msg = Json.encodeToString(openPlayerMsg)
            connectionsAdvertiser.sendMessageToMultiple(msg = msg, receivers = to)
        }
    }
}

data class PlaybackSetupState(
    val canChangePlaybackSettings: Boolean,
)

interface PlaybackSetupEvents {
    var onOpenPlayerMessage: (() -> Unit)?
}

@Serializable
open class VisyncMessage(
    val type: String
)

@Serializable
class TextMessage(
    val message: String
) : VisyncMessage(
    type = TextMessage::class.simpleName!!
)

@Serializable
class VersionMessage(
    val version: Int,
) : VisyncMessage(
    type = VersionMessage::class.simpleName!!
)

@Serializable
class OpenPlayerMessage(

) : VisyncMessage(
    type = OpenPlayerMessage::class.simpleName!!
)

