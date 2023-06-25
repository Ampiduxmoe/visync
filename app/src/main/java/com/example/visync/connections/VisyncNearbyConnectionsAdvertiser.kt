package com.example.visync.connections

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface VisyncNearbyConnectionsAdvertiser {
    val advertiserState: StateFlow<AdvertiserState>
    fun startAdvertising(username: String, context: Context)
    fun stopAdvertising()
}

interface AdvertiserState {
    val isAdvertising: Boolean
    val connectionRequests: List<ConnectionRequest>
    val runningConnections: List<RunningConnection>
    val messages: List<String>
}