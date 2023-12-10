package com.example.visync.ui.screens.player

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.example.visync.connections.EmptyVisyncNearbyConnectionsListener
import com.example.visync.connections.RunningConnection
import com.example.visync.connections.VisyncNearbyConnections
import com.example.visync.data.videofiles.Videofile
import com.example.visync.messaging.JsonVisyncMessageConverter
import com.example.visync.messaging.PingMessage
import com.example.visync.messaging.PlaybackPauseUnpauseMessage
import com.example.visync.messaging.PlaybackSeekToMessage
import com.example.visync.messaging.PlaybackSeekToPrevNextMessage
import com.example.visync.messaging.PongMessage
import com.example.visync.messaging.VisyncMessage
import com.example.visync.player.PlayerWrapper
import com.example.visync.ui.screens.main.playback_setup.PingData
import com.example.visync.ui.screens.main.playback_setup.SingleEndpointPings
import com.example.visync.ui.screens.main.playback_setup.getCurrentTimestamp
import com.example.visync.ui.screens.main.playback_setup.withReplacedValueFoundByReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VisyncPlayerViewModel @Inject constructor(
    val playerWrapper: PlayerWrapper,
    private val connections: VisyncNearbyConnections,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VisyncPlayerUiState(
            currentPlaylist = mapOf(),
            selectedVideofile = null,
            isOverlayVisible = false
        )
    )
    val uiState: StateFlow<VisyncPlayerUiState> = _uiState

    /** Host-only state */
    private val _pingData = MutableStateFlow(
        emptyList<SingleEndpointPings>()
    )
    val pingState: StateFlow<List<SingleEndpointPings>> = _pingData

    private val mainHandler = Handler(Looper.getMainLooper())
    private val updateCurrentPositionTask = object : Runnable {
        override fun run() {
            val pollingInterval = playerWrapper.playbackState.value.currentPositionPollingInterval
            playerWrapper.playbackStateSetters.setCurrentPosition(
                playerWrapper.getPlayer().currentPosition
            )
            mainHandler.postDelayed(this, pollingInterval.toLong())
        }
    }

    init {
        val eventListener = playerWrapper.buildEventListener(
            setSelectedMediaItem = this::setSelectedVideofileByMediaItem,
            onIsPlayingChangedAction = { isPlaying ->
                if (isPlaying) {
                    mainHandler.post(updateCurrentPositionTask)
                } else {
                    mainHandler.removeCallbacks(updateCurrentPositionTask)
                }
            }
        )
        playerWrapper.setPlayerListener(eventListener)
    }

    private val _messageConverter = JsonVisyncMessageConverter()
    private val _hostListener = object : EmptyVisyncNearbyConnectionsListener() {
        override fun onNewMessage(message: String, from: RunningConnection) {
            val fullMessage = _messageConverter.decode(message)
            Log.d("Test", "processing message from inside a host player listener")
            when (fullMessage) {
                is PongMessage -> {
                    val pongTimestamp = getCurrentTimestamp()
                    val oldWatcherPingsEntry = _pingData.value
                        .find { it.endpointId == from.endpointId } ?: return
                    val oldPingEntry = oldWatcherPingsEntry.pingData.pingList
                        .find { it.requestTimestamp == fullMessage.pingTimestamp } ?: return
                    val newPingEntry = oldPingEntry.copy(
                        responseTimestamp = pongTimestamp
                    )
                    val newWatcherPingsEntry = oldWatcherPingsEntry.copy(
                        pingData = oldWatcherPingsEntry.pingData.withReplacedEntry(
                            entry = oldPingEntry,
                            newEntry = newPingEntry
                        )
                    )
                    _pingData.update {
                        it.withReplacedValueFoundByReference(
                            value = oldWatcherPingsEntry,
                            newValue = newWatcherPingsEntry
                        )
                    }
                }
            }
        }
    }

    private var _pingJob: Job? = null
    fun startPinging() {
        if (_pingJob != null) { return }
        _pingJob = viewModelScope.launch {
            while (true) {
                val knownEndpointIds = _pingData.value.map { it.endpointId }
                val runningConnections = connections.connectionsState.value.runningConnections
                val currentEndpointIds = runningConnections.map { it.endpointId }
                val newEndpointIds = currentEndpointIds.filter { it !in knownEndpointIds }
                val newEntries = newEndpointIds.map { SingleEndpointPings(it, PingData()) }
                if (newEntries.isNotEmpty()) {
                    _pingData.update { it + newEntries }
                }

                val danglingIds = knownEndpointIds.filter { it !in currentEndpointIds }
                if (danglingIds.isNotEmpty()) {
                    _pingData.update { pingData -> pingData.filter { it.endpointId in currentEndpointIds } }
                }

                val currentTimestamp = getCurrentTimestamp()
                val pingMessage = PingMessage(currentTimestamp)
                val msg = _messageConverter.encode(pingMessage)
                connections.sendMessageToMultiple(msg, knownEndpointIds)
                _pingData.update {
                    it.map { endpointPings ->
                        endpointPings.withUnansweredPing(
                            pingTimestamp = currentTimestamp
                        )
                    }
                }
                Log.d("Test", _pingData.value.toString())
                delay(1000L)
            }
        }
    }

    fun stopPinging() {
        _pingJob?.cancel()
        _pingJob = null
    }

    fun listenToNearbyConnectionsAsHost() {
        stopListeningToNearbyConnections()
        Log.d("Test", "started listening from player as host")
        connections.addEventListener(_hostListener)
    }

    private val _guestListener = object : EmptyVisyncNearbyConnectionsListener() {
        override fun onNewMessage(message: String, from: RunningConnection) {
            val fullMessage = _messageConverter.decode(message)
            val playbackControls = playerWrapper.playbackControls
            Log.d("Test", "processing message from inside a guest player listener")
            when (fullMessage) {
                is PlaybackPauseUnpauseMessage -> {
                    val delay = fullMessage.pingList[0].pingData.weightedAverage // TODO: we need to know who we are to know the delay
                    if (fullMessage.doPause) {
                        playbackControls.pause()
                        playbackControls.seekTo(fullMessage.position)
                    } else {
                        playbackControls.unpause()
                    }
                }
                is PlaybackSeekToMessage -> {
                    playbackControls.seekTo(fullMessage.seekTo)
                }
                is PlaybackSeekToPrevNextMessage -> {
                    if (fullMessage.toPrev) {
                        playbackControls.seekToPrevious()
                    } else {
                        playbackControls.seekToNext()
                    }
                }
                is PingMessage -> {
                    val pongMessage = PongMessage(fullMessage.pingTimestamp)
                    val msg = _messageConverter.encode(pongMessage)
                    from.sendMessage(msg)
                }
            }
        }
    }
    fun listenToNearbyConnectionsAsGuest() {
        stopListeningToNearbyConnections()
        Log.d("Test", "started listening from player as guest")
        connections.addEventListener(_guestListener)
    }

    fun stopListeningToNearbyConnections() {
        connections.removeEventListener(_hostListener)
        connections.removeEventListener(_guestListener)
    }

    val hostPlayerMessenger = object : HostPlayerMessenger {

        private fun getActiveEndpointIds(): List<String> {
            return connections.connectionsState.value.runningConnections.map {
                it.endpointId
            }
        }

        private inline fun <reified T: VisyncMessage> encodeAndSendToActive(message: T) {
            val msg = _messageConverter.encode(message)
            connections.sendMessageToMultiple(
                msg = msg,
                endpointIds = getActiveEndpointIds()
            )
        }

        override fun getPingData(): List<SingleEndpointPings> { // ofc this should not be here but anyways, to get that prototype faster
            return _pingData.value
        }

        override fun sendPause() {
            val pauseUnpauseMessage = PlaybackPauseUnpauseMessage(
                pingList = _pingData.value,
                doPause = true,
                position = playerWrapper.getPlayer().currentPosition
            )
            encodeAndSendToActive(pauseUnpauseMessage)
        }
        override fun sendUnpause() {
            val pauseUnpauseMessage = PlaybackPauseUnpauseMessage(
                pingList = _pingData.value,
                doPause = false,
                position = playerWrapper.getPlayer().currentPosition
            )
            encodeAndSendToActive(pauseUnpauseMessage)
        }
        override fun sendSeekTo(seekTo: Long) {
            val seekToMessage = PlaybackSeekToMessage(
                pingList = _pingData.value,
                seekTo = seekTo
            )
            encodeAndSendToActive(seekToMessage)
        }
    }

    fun setVideofilesToPlay(
        videofilesToPlay: List<Videofile>,
        startFrom: Int,
    ) {
        val selectedVideofile = videofilesToPlay[startFrom]
        val noDummyVideofiles = videofilesToPlay.filter { it.uri != Uri.EMPTY }
        _uiState.value = _uiState.value.copy(
            currentPlaylist = noDummyVideofiles
                .associateWith(::videofileToMediaItem)
                .also { videofilesToMediaItems ->
                    playerWrapper.getPlayer().setMediaItems(
                        /* mediaItems = */ videofilesToMediaItems.values.toList(),
                        /* startIndex = */ maxOf(noDummyVideofiles.indexOf(selectedVideofile),0),
                        /* startPositionMs = */ 0
                    )
                },
            selectedVideofile = selectedVideofile
        )
    }

    private fun videofileToMediaItem(videofile: Videofile): MediaItem {
        return MediaItem.fromUri(videofile.uri)
    }

    private fun setSelectedVideofileByMediaItem(mediaItem: MediaItem?) {
        val newSelectedVideofile = _uiState.value
            .currentPlaylist
            .filter { it.value == mediaItem }
            .keys
            .firstOrNull()
        if (_uiState.value.selectedVideofile == newSelectedVideofile) {
            return
        }
        _uiState.value = _uiState.value.copy(
            selectedVideofile = newSelectedVideofile
        )
    }

    fun hideOverlay() {
        _uiState.value = _uiState.value.copy(
            isOverlayVisible = false
        )
    }

    fun showOverlay() {
        _uiState.value = _uiState.value.copy(
            isOverlayVisible = true
        )
    }

    override fun onCleared() {
        super.onCleared()
        playerWrapper.removePlayerListener()
        playerWrapper.getPlayer().release()
    }
}

data class VisyncPlayerUiState(
    val currentPlaylist: Map<Videofile, MediaItem>,
    val selectedVideofile: Videofile?,
    val isOverlayVisible: Boolean,
)

interface HostPlayerMessenger {
    fun getPingData(): List<SingleEndpointPings>
    fun sendPause()
    fun sendUnpause()
    fun sendSeekTo(seekTo: Long)
}