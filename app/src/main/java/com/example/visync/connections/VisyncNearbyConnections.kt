package com.example.visync.connections

import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import kotlinx.coroutines.flow.StateFlow

interface VisyncNearbyConnections {
    val connectionsState: StateFlow<VisyncNearbyConnectionsState>
    var eventListener: VisyncNearbyConnectionsListener

    fun initialize(config: VisyncNearbyConnectionsConfiguration)
    fun startAdvertising(
        config: VisyncNearbyConnectionsConfiguration? = null,
        onTaskFailure: (Exception) -> Unit = {},
        onTaskSuccess: () -> Unit = {},
    )
    fun stopAdvertising()
    fun startDiscovering(
        config: VisyncNearbyConnectionsConfiguration? = null,
        onTaskFailure: (Exception) -> Unit = {},
        onTaskSuccess: () -> Unit = {},
    )
    fun stopDiscovering()
    fun sendMessageToMultiple(
        msg: String,
        endpointIds: List<String>,
        onTaskFailure: (Exception) -> Unit = {},
        onTaskSuccess: () -> Unit = {},
    )
    fun resetToIdle()
}

data class VisyncNearbyConnectionsState (
    val broadcastingState: BroadcastingState,
    val discoveredEndpoints: List<DiscoveredEndpoint>,
    val connectionRequests: List<ConnectionRequest>,
    val runningConnections: List<RunningConnection>,
) {
    val isAdvertising: Boolean
        get() = broadcastingState == BroadcastingState.ADVERTISING
    val isDiscovering: Boolean
        get() = broadcastingState == BroadcastingState.DISCOVERING
}

data class VisyncNearbyConnectionsConfiguration(
    val username: String
)

interface VisyncNearbyConnectionsListener {
    fun onNewDiscoveredEndpoint(endpoint: DiscoveredEndpoint)
    fun onNewConnectionRequest(request: ConnectionRequest)
    fun onNewRunningConnection(connection: RunningConnection)
    fun onRunningConnectionLost(connection: RunningConnection)
    fun onNewMessage(message: String, from: RunningConnection)
}

open class EmptyVisyncNearbyConnectionsListener : VisyncNearbyConnectionsListener {
    override fun onNewDiscoveredEndpoint(endpoint: DiscoveredEndpoint) { }
    override fun onNewConnectionRequest(request: ConnectionRequest) { }
    override fun onNewRunningConnection(connection: RunningConnection) { }
    override fun onRunningConnectionLost(connection: RunningConnection) { }
    override fun onNewMessage(message: String, from: RunningConnection) { }
}

interface DiscoveredEndpoint {
    val endpointId: String
    val endpointInfo: DiscoveredEndpointInfo
    fun initiateConnection(
        onTaskFailure: (Exception) -> Unit = {},
        onTaskSuccess: () -> Unit = {},
    )
}

interface ConnectionRequest {
    val endpointId: String
    val connectionInfo: ConnectionInfo
    fun accept(
        onTaskFailure: (Exception) -> Unit = {},
        onTaskSuccess: () -> Unit = {},
        onConnectionResult: (ConnectionResolution) -> Unit = {},
    )
    fun reject(
        onTaskFailure: (Exception) -> Unit = {},
        onTaskSuccess: () -> Unit = {},
    )
}

interface RunningConnection {
    val endpointId: String
    val username: String
    fun sendMessage(
        msg: String,
        onTaskFailure: (Exception) -> Unit = {},
        onTaskSuccess: () -> Unit = {},
    )
    fun disconnect()
}

enum class BroadcastingState {
    IDLE, ADVERTISING, DISCOVERING
}