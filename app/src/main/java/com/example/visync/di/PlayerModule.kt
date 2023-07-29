package com.example.visync.di

import android.app.Application
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.visync.player.DefaultPlayerWrapper
import com.example.visync.player.PlayerWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Singleton
    @Provides
    fun providePlayer(app: Application): Player {
        return ExoPlayer.Builder(app).build()
    }

    @Singleton
    @Provides
    fun providePlayerWrapper(player: Player): PlayerWrapper {
        return DefaultPlayerWrapper(player)
    }
}