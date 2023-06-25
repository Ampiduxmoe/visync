package com.example.visync.connections

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface VisyncNearbyConnectionsDiscoverer {
    val discovererState: StateFlow<DiscovererState>
    fun startDiscovering(username: String, context: Context)
    fun stopDiscovering()
}

interface DiscovererState {
    val isDiscovering: Boolean
    val discoveredEndpoints: List<DiscoveredEndpoint>
    val connectionRequests: List<ConnectionRequest>
    val runningConnections: List<RunningConnection>
    val messages: List<String>
}