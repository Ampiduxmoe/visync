package com.example.visync.di

import com.example.visync.data.videofiles.FakeVideofilesRepository
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
    fun provideVideofilesRepository(): VideofilesRepository {
        return FakeVideofilesRepository()
    }
}