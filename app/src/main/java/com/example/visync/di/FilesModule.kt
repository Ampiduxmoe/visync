package com.example.visync.di

import android.app.Application
import com.example.visync.data.files.VideoMetadataReader
import com.example.visync.data.files.VisyncVideoMetadataReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object FilesModule {

    @Provides
    fun provideVideoMetadataReader(app: Application): VideoMetadataReader {
        return VisyncVideoMetadataReader(app)
    }
}