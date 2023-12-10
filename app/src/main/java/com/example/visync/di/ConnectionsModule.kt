package com.example.visync.di

import android.app.Application
import com.example.visync.connections.VisyncNearbyConnections
import com.example.visync.connections.VisyncNearbyConnectionsImpl
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionsClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConnectionsModule {

    @Singleton
    @Provides
    fun provideConnectionsClient(app: Application): ConnectionsClient {
        return Nearby.getConnectionsClient(app)
    }

    @Singleton
    @Provides
    fun provideVisyncNearbyConnections(
        connectionsClient: ConnectionsClient,
        app: Application,
    ): VisyncNearbyConnections {
        return VisyncNearbyConnectionsImpl(connectionsClient, app)
    }
}