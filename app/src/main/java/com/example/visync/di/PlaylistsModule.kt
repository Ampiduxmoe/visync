package com.example.visync.di

import com.example.visync.data.playlists.DbPlaylistsRepository
import com.example.visync.data.playlists.PlaylistDao
import com.example.visync.data.playlists.PlaylistsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaylistsModule {

    @Singleton
    @Provides
    fun providePlaylistsRepository(playlistsDao: PlaylistDao): PlaylistsRepository {
        return DbPlaylistsRepository(playlistsDao)
    }
}