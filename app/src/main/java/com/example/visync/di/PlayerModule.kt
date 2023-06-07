package com.example.visync.di

import android.app.Application
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    fun providePlayer(app: Application): Player {
        return ExoPlayer.Builder(app).build()
    }
}