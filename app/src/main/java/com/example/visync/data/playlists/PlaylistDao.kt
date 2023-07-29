package com.example.visync.data.playlists

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg playlists: Playlist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertNewOnly(vararg playlists: Playlist)

    @Delete
    fun delete(vararg playlists: Playlist)

    @Update
    fun update(vararg playlists: Playlist)

    @Transaction
    @Query("select * from playlist where playlistId = :id")
    fun getByIdWithVideofiles(id: Long): Flow<PlaylistWithVideofiles?>

    @Query("select * from playlist")
    fun getAll(): Flow<List<Playlist>>
}