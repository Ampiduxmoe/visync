package com.example.visync.connections

import android.app.Application
import android.util.Log
import com.example.visync.R
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class VisyncNearbyConnectionsImpl @Inject constructor(
    private val _connectionsClient: ConnectionsClient,
    private val _application: Application
) : VisyncNearbyConnections {

    private val defaultConnectionsState = VisyncNearbyConnectionsState(
        broadcastingState = BroadcastingState.IDLE,
        discoveredEndpoints = listOf(),
        connectionRequests = listOf(),
        runningConnections = listOf(),
    )

    private val _connectionsState = MutableStateFlow(defaultConnectionsState)
    override val connectionsState = _connectionsState.asStateFlow()

    private var _broadcastingState
        get() = _connectionsState.value.broadcastingState
        set(value) {
            if (value == _broadcastingState) { return }
            _connectionsState.update { it.copy(broadcastingState = value) }
            eventListeners.forEach { it.onBroadcastingStateChanged(value) }
        }
    private var _discoveredEndpoints
        get() = _connectionsState.value.discoveredEndpoints
        set(value) = _connectionsState.update { it.copy(discoveredEndpoints = value) }
    private var _connectionRequests
        get() = _connectionsState.value.connectionRequests
        set(value) = _connectionsState.update { it.copy(connectionRequests = value) }
    private var _runningConnections
        get() = _connectionsState.value.runningConnections
        set(value) = _connectionsState.update { it.copy(runningConnections = value) }

    private var eventListeners: MutableList<VisyncNearbyConnectionsListener> = mutableListOf()

    private var _stopAdvertising: (() -> Unit)? = null
    private var _stopDiscovering: (() -> Unit)? = null

    private var _config: VisyncNearbyConnectionsConfiguration? = null

    override fun initialize(config: VisyncNearbyConnectionsConfiguration) {
        _config = config
    }

    override fun startAdvertising(
        config: VisyncNearbyConnectionsConfiguration?,
        onTaskFailure: (Exception) -> Unit,
        onTaskSuccess: () -> Unit,
    ) {
        config?.let { _config = config }
        if (_broadcastingState != BroadcastingState.IDLE) {
            return
        }
        val currentConfig = _config ?: throw Exception("No configuration found")
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
        _connectionsClient
            .startAdvertising(
                currentConfig.username,
                _application.getString(R.string.app_name),
                connectionLifecycleCallback,
                advertisingOptions
            )
            .addOnSuccessListener {
                logD("Started advertising")
                _broadcastingState = BroadcastingState.ADVERTISING
                _stopAdvertising = {
                    _connectionsClient.stopAdvertising()
                    _broadcastingState = BroadcastingState.IDLE
                    _stopAdvertising = null
                    logD("Stopped advertising")
                }
                onTaskSuccess()
            }
            .addOnFailureListener { exception ->
                logE("Failed to start advertising", exception)
                onTaskFailure(exception)
            }
    }

    override fun stopAdvertising() {
        _stopAdvertising?.invoke()
    }

    override fun startDiscovering(
        config: VisyncNearbyConnectionsConfiguration?,
        onTaskFailure: (Exception) -> Unit,
        onTaskSuccess: () -> Unit,
    ) {
        config?.let { _config = config }
        if (_broadcastingState != BroadcastingState.IDLE) {
            return
        }
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
        _connectionsClient
            .startDiscovery(
                _application.getString(R.string.app_name),
                endpointDiscoveryCallback,
                discoveryOptions
            )
            .addOnSuccessListener {
                logD("Started discovering")
                _discoveredEndpoints = emptyList()
                _broadcastingState = BroadcastingState.DISCOVERING
                _stopDiscovering = {
                    _connectionsClient.stopDiscovery()
                    _broadcastingState = BroadcastingState.IDLE
                    _stopDiscovering = null
                    logD("Stopped discovering")
                }
            }
            .addOnFailureListener { exception ->
                logE("Failed to start discovering", exception)
            }
    }

    override fun stopDiscovering() {
        _stopDiscovering?.invoke()
    }

    private fun sendMessage(
        msg: String,
        receiver: RunningConnection,
        onTaskFailure: (Exception) -> Unit,
        onTaskSuccess: () -> Unit,
    ) {
        val payload = Payload.fromBytes(msg.toByteArray(Charsets.UTF_8))
        _connectionsClient
            .sendPayload(receiver.endpointId, payload)
            .addOnSuccessListener {
                val receiverTag = "${receiver.endpointId} (${receiver.username})"
                logD("Successfully sent a message ($msg) to $receiverTag")
                onTaskSuccess()
            }
            .addOnFailureListener { exception ->
                logE("Could not send a message", exception)
                onTaskFailure(exception)
            }
    }

    override fun sendMessageToMultiple(
        msg: String,
        endpointIds: List<String>,
        onTaskFailure: (Exception) -> Unit,
        onTaskSuccess: () -> Unit,
    ) {
        endpointIds.ifEmpty { return }
        val payload = Payload.fromBytes(msg.toByteArray(Charsets.UTF_8))
        _connectionsClient
            .sendPayload(endpointIds, payload)
            .addOnSuccessListener {
                logD("Successfully sent a multicast message ($msg) to $endpointIds")
                onTaskSuccess()
            }
            .addOnFailureListener { exception ->
                logE("Could not send a message", exception)
                onTaskFailure(exception)
            }
    }

    override fun tryInitiateConnection(
        endpointId: String,
        onTaskFailure: (Exception) -> Unit,
        onTaskSuccess: () -> Unit,
    ) {
        val currentConfig = _config ?: throw Exception("No configuration found")
        logD("Trying to connect directly through Endpoint ID to $endpointId")
        _connectionsClient
            .requestConnection(
                currentConfig.username,
                endpointId,
                connectionLifecycleCallback
            )
            .addOnSuccessListener {
                logD("Successfully sent direct connection request to $endpointId")
                onTaskSuccess()
            }
            .addOnFailureListener { exception ->
                logE("Failed to send direct connection request to $endpointId", exception)
                onTaskFailure(exception)
            }
    }

    override fun resetToIdle() {
        stopAdvertising()
        stopDiscovering()
        _connectionsClient.stopAllEndpoints()
        _connectionsState.value = defaultConnectionsState
    }

    override fun addEventListener(listener: VisyncNearbyConnectionsListener) {
        eventListeners += listener
    }

    override fun removeEventListener(listener: VisyncNearbyConnectionsListener) {
        eventListeners -= listener
    }

    private fun addDiscoveredEndpoint(
        endpointId: String,
        endpointInfo: DiscoveredEndpointInfo,
    ) {
        val discoveredEndpoint = DiscoveredEndpointImpl(
            endpointId = endpointId,
            endpointInfo = endpointInfo,
        )
        _discoveredEndpoints = _discoveredEndpoints + discoveredEndpoint
        eventListeners.forEach { it.onNewDiscoveredEndpoint(discoveredEndpoint) }
    }

    private fun removeDiscoveredEndpoint(endpointId: String) {
        _discoveredEndpoints = _discoveredEndpoints
            .filter { it.endpointId != endpointId }
    }

    private fun addConnectionRequest(
        endpointId: String,
        connectionInfo: ConnectionInfo,
    ) {
        val connectionRequest = ConnectionRequestImpl(
            endpointId = endpointId,
            connectionInfo = connectionInfo,
        )
        _connectionRequests = _connectionRequests + connectionRequest
        eventListeners.forEach { it.onNewConnectionRequest(connectionRequest) }
    }

    private fun removeConnectionRequest(endpointId: String) {
        _connectionRequests = _connectionRequests
            .filter { it.endpointId != endpointId }
    }

    private fun addRunningConnection(
        endpointId: String,
        username: String,
    ) {
        val runningConnection = RunningConnectionImpl(
            endpointId = endpointId,
            username = username
        )
        _runningConnections = _runningConnections + runningConnection
        eventListeners.forEach { it.onNewRunningConnection(runningConnection) }
    }

    private fun removeRunningConnection(endpointId: String) {
        val runningConnection = _runningConnections
            .find { it.endpointId == endpointId } ?: return
        _runningConnections = _runningConnections
            .filter { it != runningConnection }
        eventListeners.forEach { it.onRunningConnectionLost(runningConnection) }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            logD("$endpointId (${info.endpointName}) initiated connection")
            addConnectionRequest(
                endpointId = endpointId,
                connectionInfo = info
            )
        }
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            val connectionRequest = _connectionRequests.find { it.endpointId == endpointId }
            if (connectionRequest == null) {
                val warningMessage = (
                    "No associated ConnectionRequest found after receiving connection result. " +
                    "This may result an untracked connection, " +
                    "so connection will be closed immediately."
                )
                logW(warningMessage)
                _connectionsClient.disconnectFromEndpoint(endpointId)
                return
            }
            val endpointName = connectionRequest.connectionInfo.endpointName
            val connectionTag = "$endpointId (${endpointName})"
            when (val statusCode = result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    logD("Connected to $connectionTag")
                    removeConnectionRequest(endpointId)
                    addRunningConnection(
                        endpointId = endpointId,
                        username = endpointName
                    )
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    logD("Connection to $connectionTag was rejected")
                    removeConnectionRequest(endpointId)
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    logW("Result of connection to $connectionTag is STATUS_ERROR ($statusCode)")
                    removeConnectionRequest(endpointId)
                }
                else -> {
                    logW("Result of connection to $connectionTag is status code of $statusCode")
                    removeConnectionRequest(endpointId)
                }
            }
            if (connectionRequest is ConnectionRequestImpl) {
                connectionRequest.onConnectionResult?.invoke(result)
            }
        }

        override fun onDisconnected(endpointId: String) {
            logD("Disconnected from $endpointId")
            removeRunningConnection(endpointId)
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            logD("Endpoint found: $endpointId (${info.endpointName})")
            addDiscoveredEndpoint(
                endpointId = endpointId,
                endpointInfo = info,
            )
        }
        override fun onEndpointLost(endpointId: String) {
            logD("Endpoint lost: $endpointId")
            removeDiscoveredEndpoint(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                val msg = String(bytes, Charsets.UTF_8)
                logD("Received message ($msg)")
                val senderConnection = _runningConnections.find { it.endpointId == endpointId }
                if (senderConnection == null){
                    val warningMessage = (
                        "Received message was from an untracked endpoint. " +
                        "Connection will be closed immediately."
                    )
                    logW(warningMessage)
                    _connectionsClient.disconnectFromEndpoint(endpointId)
                    return
                }
                eventListeners.forEach { it.onNewMessage(msg, senderConnection) }
            }
        }
        override fun onPayloadTransferUpdate(
            endpointId: String,
            transferUpdate: PayloadTransferUpdate
        ) {

        }
    }

    private inner class DiscoveredEndpointImpl(
        override val endpointId: String,
        override val endpointInfo: DiscoveredEndpointInfo,
    ) : DiscoveredEndpoint {
        override fun initiateConnection(
            onTaskFailure: (Exception) -> Unit,
            onTaskSuccess: () -> Unit,
        ) {
            val currentConfig = _config ?: throw Exception("No configuration found")
            val connectionTag = "$endpointId (${endpointInfo.endpointName})"
            _connectionsClient
                .requestConnection(
                    currentConfig.username,
                    endpointId,
                    connectionLifecycleCallback
                )
                .addOnSuccessListener {
                    logD("Successfully sent connection request to $connectionTag")
                    removeDiscoveredEndpoint(endpointId)
                    onTaskSuccess()
                }
                .addOnFailureListener { exception ->
                    logE("Failed to send connection request to $connectionTag", exception)
                    onTaskFailure(exception)
                }
        }
    }

    private inner class ConnectionRequestImpl(
        override val endpointId: String,
        override val connectionInfo: ConnectionInfo,
    ) : ConnectionRequest {
        var onConnectionResult: ((ConnectionResolution) -> Unit)? = null
            private set

        private var _isPending = false

        override fun accept(
            onTaskFailure: (Exception) -> Unit,
            onTaskSuccess: () -> Unit,
            onConnectionResult: (ConnectionResolution) -> Unit,
        ) {
            if (_isPending) {
                return
            }
            _isPending = true
            this.onConnectionResult = onConnectionResult
            val connectionTag = "$endpointId (${connectionInfo.endpointName})"
            _connectionsClient
                .acceptConnection(endpointId, payloadCallback)
                .addOnSuccessListener {
                    logD("Successfully sent connection accept message to $connectionTag")
                    onTaskSuccess()
                    _isPending = false
                }
                .addOnFailureListener { exception ->
                    logE("Failed to send connection accept message to $connectionTag", exception)
                    onTaskFailure(exception)
                    _isPending = false
                }
        }

        override fun reject(
            onTaskFailure: (Exception) -> Unit,
            onTaskSuccess: () -> Unit,
        ) {
            if (_isPending) {
                return
            }
            _isPending = true
            val connectionTag = "$endpointId (${connectionInfo.endpointName})"
            _connectionsClient
                .rejectConnection(endpointId)
                .addOnSuccessListener {
                    logD("Successfully sent connection reject message to $connectionTag")
                    onTaskSuccess()
                    _isPending = false
                }
                .addOnFailureListener { exception ->
                    logE("Failed to send connection reject message to $connectionTag", exception)
                    onTaskFailure(exception)
                    _isPending = false
                }
        }
    }

    private inner class RunningConnectionImpl(
        override val endpointId: String,
        override val username: String,
    ) : RunningConnection {
        override fun sendMessage(
            msg: String,
            onTaskFailure: (Exception) -> Unit,
            onTaskSuccess: () -> Unit,
        ) {
            sendMessage(
                msg = msg,
                receiver = this,
                onTaskFailure = onTaskFailure,
                onTaskSuccess = onTaskSuccess,
            )
        }

        override fun disconnect() {
            _connectionsClient.disconnectFromEndpoint(endpointId)
            removeRunningConnection(endpointId)
        }
    }

    private fun logD(msg: String) {
        Log.d(VisyncNearbyConnectionsImpl::class.simpleName, msg)
    }

    private fun logW(msg: String) {
        Log.w(VisyncNearbyConnectionsImpl::class.simpleName, msg)
    }

    private fun logE(msg: String, e: Exception) {
        Log.e(VisyncNearbyConnectionsImpl::class.simpleName, msg, e)
    }
}
