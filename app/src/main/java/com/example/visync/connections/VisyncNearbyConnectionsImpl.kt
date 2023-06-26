package com.example.visync.connections

import android.content.Context
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
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class VisyncNearbyConnectionsImpl @Inject constructor(
    private val connectionsClient: ConnectionsClient,
) : BasicVisyncNearbyConnectionsState,
    VisyncNearbyConnectionsAdvertiser,
    VisyncNearbyConnectionsDiscoverer {

    private val cleanConnectionsState = NearbyConnectionsState(
        status = ConnectionStatus.IDLE,
        discoveredEndpoints = listOf(),
        connectionRequests = listOf(),
        runningConnections = listOf(),
    )

    private val _connectionsState = MutableStateFlow(cleanConnectionsState)
    override val connectionsState: StateFlow<NearbyConnectionsState> = _connectionsState
    override val advertiserState: StateFlow<AdvertiserState> = _connectionsState
    override val discovererState: StateFlow<DiscovererState> = _connectionsState

    override var eventListener: VisyncNearbyConnectionsListener = VisyncNearbyConnectionsListener()
        private set
    override var advertiserEventListener: VisyncAdvertiserListener = eventListener
        private set
    override var discovererEventListener: VisyncDiscovererListener = eventListener
        private set

    private var username = ""
    private val endpointUsernames = mutableMapOf<String, String>()

    private var _stopAdvertising: (() -> Unit)? = null
    private var _stopDiscovering: (() -> Unit)? = null
    private var _sendMessage: ((msg: String, to: RunningConnection) -> Unit)? = null

    override fun startAdvertising(username: String, context: Context) {
        if (_stopAdvertising != null || _stopDiscovering != null) {
            return
        }
        this.username = username
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
        connectionsClient
            .startAdvertising(
                this.username,
                context.getString(R.string.app_name),
                connectionLifecycleCallback,
                advertisingOptions
            )
            .addOnSuccessListener {
                Log.d(
                    "NearbyConnectionsWrapper",
                    "Started advertising"
                )
                setStatus(ConnectionStatus.ADVERTISING)
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "NearbyConnectionsWrapper",
                    "Failed to start advertising",
                    exception
                )
            }
        _stopAdvertising = {
            connectionsClient.stopAdvertising()
            _stopAdvertising = null
            setStatus(ConnectionStatus.IDLE)
        }
    }

    override fun stopAdvertising() {
        _stopAdvertising?.invoke()
    }

    override fun startDiscovering(username: String, context: Context) {
        if (_stopAdvertising != null || _stopDiscovering != null) {
            return
        }
        this.username = username
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
        connectionsClient
            .startDiscovery(
                context.getString(R.string.app_name),
                endpointDiscoveryCallback,
                discoveryOptions
            )
            .addOnSuccessListener {
                Log.d(
                    "NearbyConnectionsWrapper",
                    "Started discovering"
                )
                setStatus(ConnectionStatus.DISCOVERING)
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "NearbyConnectionsWrapper",
                    "Failed to start discovering",
                    exception
                )
            }
        _stopDiscovering = {
            connectionsClient.stopDiscovery()
            _stopDiscovering = null
            setStatus(ConnectionStatus.IDLE)
        }
    }

    override fun stopDiscovering() {
        _stopDiscovering?.invoke()
    }

    override fun stop() {
        stopAdvertising()
        stopDiscovering()
        connectionsClient.stopAllEndpoints()
        _connectionsState.value = cleanConnectionsState
    }

    private fun sendMessage(msg: String, receiver: RunningConnection) {
        _sendMessage?.invoke(msg, receiver)
    }

    private fun setStatus(status: ConnectionStatus) {
        val oldStatus = _connectionsState.value.status
        if (oldStatus == status) {
            return
        }
        _connectionsState.value = _connectionsState.value.copy(
            status = status
        )
        eventListener.onStatusChanged(status)
        if (oldStatus == ConnectionStatus.ADVERTISING || status == ConnectionStatus.ADVERTISING) {
            advertiserEventListener.onIsAdvertisingChanged(
                isAdvertising = status == ConnectionStatus.ADVERTISING
            )
        }
        if (oldStatus == ConnectionStatus.DISCOVERING || status == ConnectionStatus.DISCOVERING) {
            discovererEventListener.onIsDiscoveringChanged(
                isDiscovering = status == ConnectionStatus.DISCOVERING
            )
        }
    }

    private fun addDiscoveredEndpoint(
        endpointId: String,
        endpointInfo: DiscoveredEndpointInfo,
    ) {
        val discoveredUsername = endpointInfo.endpointName
        endpointUsernames += endpointId to discoveredUsername
        val discoveredEndpoint = DiscoveredEndpointImpl(
            endpointId = endpointId,
            endpointInfo = endpointInfo,
        )
        _connectionsState.value = _connectionsState.value.copy(
            discoveredEndpoints = _connectionsState.value.discoveredEndpoints + discoveredEndpoint
        )
        eventListener.onNewDiscoveredEndpoint(discoveredEndpoint)
    }

    private fun removeDiscoveredEndpoint(endpointId: String) {
        _connectionsState.value = _connectionsState.value.copy(
            discoveredEndpoints = _connectionsState.value.discoveredEndpoints
                .filter { it.endpointId != endpointId }
        )
    }

    private fun addConnectionRequest(
        endpointId: String,
        connectionInfo: ConnectionInfo,
    ) {
        endpointUsernames += endpointId to connectionInfo.endpointName
        val connectionRequest = ConnectionRequestImpl(
            endpointId = endpointId,
            connectionInfo = connectionInfo,
        )
        _connectionsState.value = _connectionsState.value.copy(
            connectionRequests = _connectionsState.value.connectionRequests + connectionRequest
        )
        eventListener.onNewConnectionRequest(connectionRequest)
    }

    private fun removeConnectionRequest(endpointId: String) {
        _connectionsState.value = _connectionsState.value.copy(
            connectionRequests = _connectionsState.value.connectionRequests
                .filter { it.endpointId != endpointId }
        )
    }

    private fun addRunningConnection(
        endpointId: String,
    ) {
        val runningConnection = RunningConnectionImpl(
            endpointId = endpointId,
            endpointUsername = endpointUsernames[endpointId]!! // we should have it by now
        )
        _connectionsState.value = _connectionsState.value.copy(
            runningConnections = _connectionsState.value.runningConnections + runningConnection
        )
        eventListener.onNewRunningConnection(runningConnection)
    }

    private fun removeRunningConnection(endpointId: String) {
        _connectionsState.value = _connectionsState.value.copy(
            runningConnections = _connectionsState.value.runningConnections
                .filter { it.endpointId != endpointId }
        )
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(
                "NearbyConnectionsWrapper",
                "$endpointId (${info.endpointName}) initiated connection"
            )
            addConnectionRequest(
                endpointId = endpointId,
                connectionInfo = info
            )
        }
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(
                        "NearbyConnectionsWrapper",
                        "Connected to $endpointId! (${endpointUsernames[endpointId]})"
                    )
                    removeConnectionRequest(endpointId)
                    addRunningConnection(
                        endpointId = endpointId
                    )
                    _sendMessage = { msg, runningConnection ->
                        val payload = Payload.fromBytes(msg.toByteArray(Charsets.UTF_8))
                        connectionsClient
                            .sendPayload(runningConnection.endpointId, payload)
                            .addOnSuccessListener {
                                val trimmedMsg = msg.substring(0, 20.coerceAtMost(msg.length))
                                val maybeThreeDots = if (msg.length > trimmedMsg.length) "..." else ""
                                Log.d(
                                    "NearbyConnectionsWrapper",
                                    "Successfully sent a message ($trimmedMsg$maybeThreeDots)"
                                )
                            }
                            .addOnFailureListener { exception ->
                                Log.e(
                                    "NearbyConnectionsWrapper",
                                    "could not send a message", exception
                                )
                            }
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(
                        "NearbyConnectionsWrapper",
                        "Connection to $endpointId (${endpointUsernames[endpointId]}) rejected"
                    )
                    removeConnectionRequest(endpointId)
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.w(
                        "NearbyConnectionsWrapper",
                        "Connection status was ${ConnectionsStatusCodes.STATUS_ERROR}"
                    )
                }
                else -> {
                    Log.w(
                        "NearbyConnectionsWrapper",
                        "Unknown status code: ${result.status.statusCode}"
                    )
                }
            }
        }
        override fun onDisconnected(endpointId: String) {
            Log.d(
                "NearbyConnectionsWrapper",
                "Disconnected from $endpointId (${endpointUsernames[endpointId]})"
            )
            removeRunningConnection(endpointId)
            _sendMessage = null
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(
                "NearbyConnectionsWrapper",
                "Endpoint found: $endpointId (${info.endpointName})"
            )
            addDiscoveredEndpoint(
                endpointId = endpointId,
                endpointInfo = info,
            )
        }
        override fun onEndpointLost(endpointId: String) {
            Log.d(
                "NearbyConnectionsWrapper",
                "Endpoint lost: $endpointId (${endpointUsernames[endpointId]})"
            )
            removeDiscoveredEndpoint(endpointId)
        }
    }

    override fun setEventListener(listener: VisyncNearbyConnectionsListener) {
        eventListener = listener
    }

    override fun setEventListener(listener: VisyncAdvertiserListener) {
        setEventListener(object : VisyncNearbyConnectionsListener() {

            override fun onIsAdvertisingChanged(isAdvertising: Boolean)
                    = listener.onIsAdvertisingChanged(isAdvertising)

            override fun onNewConnectionRequest(request: ConnectionRequest)
                    = listener.onNewConnectionRequest(request)

            override fun onNewRunningConnection(connection: RunningConnection)
                    = listener.onNewRunningConnection(connection)

            override fun onNewMessage(message: String, from: RunningConnection)
                    = listener.onNewMessage(message, from)
        })
    }

    fun asAdvertiser(): VisyncNearbyConnectionsAdvertiser = this
    fun asDiscoverer(): VisyncNearbyConnectionsDiscoverer = this

    override fun setEventListener(listener: VisyncDiscovererListener) {
        setEventListener(object : VisyncNearbyConnectionsListener() {

            override fun onIsDiscoveringChanged(isDiscovering: Boolean)
                    = listener.onIsDiscoveringChanged(isDiscovering)

            override fun onNewDiscoveredEndpoint(endpoint: DiscoveredEndpoint)
                    = listener.onNewDiscoveredEndpoint((endpoint))

            override fun onNewConnectionRequest(request: ConnectionRequest)
                    = listener.onNewConnectionRequest(request)

            override fun onNewRunningConnection(connection: RunningConnection)
                    = listener.onNewRunningConnection(connection)

            override fun onNewMessage(message: String, from: RunningConnection)
                    = listener.onNewMessage(message, from)
        })
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Log.d(
                "NearbyConnectionsWrapper",
                "Payload received from endpoint $endpointId (${endpointUsernames[endpointId]})"
            )
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { receivedBytes ->
                    val msg = String(receivedBytes, Charsets.UTF_8)
                    val trimmedMsg = msg.substring(0, 20.coerceAtMost(msg.length))
                    val maybeThreeDots = if (msg.length > trimmedMsg.length) "..." else ""
                    Log.d(
                        "NearbyConnections",
                        "Received message: $trimmedMsg$maybeThreeDots"
                    )
                    val from = connectionsState.value.runningConnections
                        .find { it.endpointId == endpointId }!!
                    eventListener.onNewMessage(msg, from)
                }
            }
        }
        override fun onPayloadTransferUpdate(
            endpointId: String,
            transferUpdate: PayloadTransferUpdate
        ) {
            Log.d(
                "NearbyConnectionsWrapper",
                "Payload transfer update"
            )
        }
    }

    private inner class DiscoveredEndpointImpl(
        override val endpointId: String,
        override val endpointInfo: DiscoveredEndpointInfo,
    ) : DiscoveredEndpoint {
        override fun initiateConnection() {
            connectionsClient
                .requestConnection(
                    username,
                    endpointId,
                    connectionLifecycleCallback
                )
                .addOnSuccessListener{
                    Log.d(
                        "NearbyConnectionsWrapper",
                        "Successfully sent connection request to " +
                                "$endpointId (${endpointInfo.endpointName})"
                    )
                    removeDiscoveredEndpoint(endpointId)
                }
                .addOnFailureListener { exception ->
                    Log.e(
                        "NearbyConnectionsWrapper",
                        "Failed to send connection request to " +
                                "$endpointId (${endpointInfo.endpointName})",
                        exception
                    )
                    removeDiscoveredEndpoint(endpointId)
                }
        }
    }

    private inner class ConnectionRequestImpl(
        override val endpointId: String,
        override val connectionInfo: ConnectionInfo,
    ) : ConnectionRequest {
        override fun accept() {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }
        override fun reject() {
            connectionsClient.rejectConnection(endpointId)
        }
    }

    private inner class RunningConnectionImpl(
        override val endpointId: String,
        override val endpointUsername: String,
    ) : RunningConnection {
        override fun sendMessage(msg: String) {
            sendMessage(msg = msg, receiver = this)
        }
        override fun disconnect() {
            connectionsClient.disconnectFromEndpoint(endpointId)
            removeRunningConnection(endpointId)
        }
    }
}