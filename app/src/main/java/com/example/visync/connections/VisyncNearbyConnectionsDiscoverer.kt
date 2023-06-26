package com.example.visync.connections

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface VisyncNearbyConnectionsDiscoverer {
    val discovererState: StateFlow<DiscovererState>
    val discovererEventListener: VisyncDiscovererListener
    fun startDiscovering(username: String, context: Context)
    fun stopDiscovering()
    fun setEventListener(listener: VisyncDiscovererListener)
    fun stop()
}

interface DiscovererState {
    val isDiscovering: Boolean
    val discoveredEndpoints: List<DiscoveredEndpoint>
    val connectionRequests: List<ConnectionRequest>
    val runningConnections: List<RunningConnection>
}

interface VisyncDiscovererListener {
    fun onIsDiscoveringChanged(isDiscovering: Boolean)
    fun onNewDiscoveredEndpoint(endpoint: DiscoveredEndpoint)
    fun onNewConnectionRequest(request: ConnectionRequest)
    fun onNewRunningConnection(connection: RunningConnection)
    fun onNewMessage(message: String, from: RunningConnection)
}