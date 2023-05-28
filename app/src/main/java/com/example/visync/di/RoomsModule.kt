package com.example.visync.di

import com.example.visync.data.rooms.FakeRoomsRepository
import com.example.visync.data.rooms.RoomsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RoomsModule {

    @Provides
    fun provideRoomsRepository(): RoomsRepository {
        return FakeRoomsRepository()
    }
}