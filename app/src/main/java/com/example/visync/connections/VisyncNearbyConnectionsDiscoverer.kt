package com.example.visync.connections

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface VisyncNearbyConnectionsDiscoverer {
    val discovererState: StateFlow<VisyncDiscovererState>
    val discovererEventListener: VisyncDiscovererListener
    fun startDiscovering(username: String, context: Context)
    fun stopDiscovering()
    fun setEventListener(listener: VisyncDiscovererListener)
    fun reset()
}

interface VisyncDiscovererState {
    val isDiscovering: Boolean
    val discoveredEndpoints: List<DiscoveredEndpoint>
    val connectionRequests: List<ConnectionRequest>
    val runningConnections: List<RunningConnection>
}

interface VisyncDiscovererListener {
    fun onIsDiscoveringChanged(isDiscovering: Boolean)
    fun onNewDiscoveredEndpoint(endpoint: DiscoveredEndpoint)
    fun onNewConnectionRequest(request: ConnectionRequest)
    fun onConnectionError(endpoint: String)
    fun onNewRunningConnection(connection: RunningConnection)
    fun onRunningConnectionLost(connection: RunningConnection)
    fun onNewMessage(message: String, from: RunningConnection)
}