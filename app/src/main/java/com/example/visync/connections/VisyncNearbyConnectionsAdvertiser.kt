package com.example.visync.connections

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface VisyncNearbyConnectionsAdvertiser {
    val advertiserState: StateFlow<VisyncAdvertiserState>
    val advertiserEventListener: VisyncAdvertiserListener
    fun startAdvertising(username: String, context: Context)
    fun stopAdvertising()
    fun sendMessageToMultiple(msg: String, receivers: List<RunningConnection>)
    fun setEventListener(listener: VisyncAdvertiserListener)
    fun reset()
}

interface VisyncAdvertiserState {
    val isAdvertising: Boolean
    val connectionRequests: List<ConnectionRequest>
    val runningConnections: List<RunningConnection>
}

interface VisyncAdvertiserListener {
    fun onIsAdvertisingChanged(isAdvertising: Boolean)
    fun onNewConnectionRequest(request: ConnectionRequest)
    fun onConnectionError(endpoint: String)
    fun onNewRunningConnection(connection: RunningConnection)
    fun onRunningConnectionLost(connection: RunningConnection)
    fun onNewMessage(message: String, from: RunningConnection)
}