package com.example.visync.di

import com.example.visync.data.videofiles.DbVideofilesRepository
import com.example.visync.data.videofiles.VideofileDao
import com.example.visync.data.videofiles.VideofilesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VideofilesModule {

    @Singleton
    @Provides
    fun provideVideofilesRepository(videofileDao: VideofileDao): VideofilesRepository {
        return DbVideofilesRepository(videofileDao)
    }
}