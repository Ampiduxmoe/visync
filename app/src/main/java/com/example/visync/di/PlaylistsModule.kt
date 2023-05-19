package com.example.visync.di

import com.example.visync.data.playlists.FakePlaylistsRepository
import com.example.visync.data.playlists.PlaylistsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object PlaylistsModule {

    @Provides
    fun providePlaylistsRepository(): PlaylistsRepository {
        return FakePlaylistsRepository()
    }
}