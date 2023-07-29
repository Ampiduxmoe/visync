package com.example.visync.di

import android.app.Application
import androidx.room.Room
import com.example.visync.data.playlists.PlaylistDao
import com.example.visync.data.videofiles.VideofileDao
import com.example.visync.db.VisyncDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val databaseName = "visync-db"

    @Singleton
    @Provides
    fun provideVisyncDatabase(app: Application): VisyncDatabase {
        return Room.databaseBuilder(
                /* context = */ app,
                /* klass = */ VisyncDatabase::class.java,
                /* name = */ databaseName
            )
            .fallbackToDestructiveMigration() // TODO: add proper migrations after release
            .build()
    }

    @Singleton
    @Provides
    fun providePlaylistDao(db: VisyncDatabase): PlaylistDao {
        return db.playlistDao()
    }

    @Singleton
    @Provides
    fun provideVideofileDao(db: VisyncDatabase): VideofileDao {
        return db.videofileDao()
    }
}