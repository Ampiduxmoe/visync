package com.example.visync.di

import com.example.visync.data.rooms.FakeRoomsRepository
import com.example.visync.data.rooms.RoomsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomsModule {

    @Singleton
    @Provides
    fun provideRoomsRepository(): RoomsRepository {
        return FakeRoomsRepository()
    }
}