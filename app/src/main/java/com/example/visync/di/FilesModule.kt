package com.example.visync.di

import android.app.Application
import com.example.visync.metadata.MetadataReader
import com.example.visync.metadata.VisyncMetadataReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object FilesModule {

    @Provides
    fun provideVideoMetadataReader(app: Application): MetadataReader {
        return VisyncMetadataReader(app)
    }
}