package com.example.visync.connections

import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import kotlinx.coroutines.flow.StateFlow

interface BasicVisyncNearbyConnectionsState {
    val connectionsState: StateFlow<NearbyConnectionsState>
    val eventListener: VisyncNearbyConnectionsListener
    fun setEventListener(listener: VisyncNearbyConnectionsListener)
}

data class NearbyConnectionsState (
    val status: ConnectionStatus,
    override val discoveredEndpoints: List<DiscoveredEndpoint>,
    override val connectionRequests: List<ConnectionRequest>,
    override val runningConnections: List<RunningConnection>,
) : AdvertiserState, DiscovererState {
    override val isAdvertising: Boolean
        get() = status == ConnectionStatus.ADVERTISING
    override val isDiscovering: Boolean
        get() = status == ConnectionStatus.DISCOVERING
}

open class VisyncNearbyConnectionsListener(

) : VisyncAdvertiserListener, VisyncDiscovererListener {
    fun onStatusChanged(status: ConnectionStatus) { }
    override fun onIsAdvertisingChanged(isAdvertising: Boolean) { }
    override fun onIsDiscoveringChanged(isDiscovering: Boolean) { }
    override fun onNewDiscoveredEndpoint(endpoint: DiscoveredEndpoint) { }
    override fun onNewConnectionRequest(request: ConnectionRequest) { }
    override fun onNewRunningConnection(connection: RunningConnection) { }
    override fun onNewMessage(message: String, from: RunningConnection) { }
}

interface DiscoveredEndpoint {
    val endpointId: String
    val endpointInfo: DiscoveredEndpointInfo
    fun initiateConnection()
}

interface ConnectionRequest {
    val endpointId: String
    val connectionInfo: ConnectionInfo
    fun accept()
    fun reject()
}

interface RunningConnection {
    val endpointId: String
    val endpointUsername: String
    fun sendMessage(msg: String)
    fun disconnect()
}

enum class ConnectionStatus {
    IDLE, ADVERTISING, DISCOVERING
}