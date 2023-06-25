package com.example.visync.di

import android.app.Application
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionsClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ConnectionsModule {

    @Provides
    fun provideConnectionsClient(app: Application): ConnectionsClient {
        return Nearby.getConnectionsClient(app)
    }
}