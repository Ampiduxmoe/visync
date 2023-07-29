package com.example.visync.data.videofiles

import android.net.Uri
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.visync.data.relations.PlaylistVideofileCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface VideofileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg videofiles: Videofile): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertNewOnly(vararg videofiles: Videofile): List<Long>

    @Delete
    fun delete(vararg videofiles: Videofile)

    @Update
    fun update(vararg videofiles: Videofile)

    @Query("select * from videofile")
    fun getAll(): Flow<List<Videofile>>

    @Query("select uri from videofile where uri in (:uris)")
    fun selectExistingFromUris(vararg uris: Uri): List<Uri>

    @Insert
    fun addPlaylistCrossRefs(vararg playlistVideofileCrossRefs: PlaylistVideofileCrossRef)

    @Query("select count(*) from playlistvideofilecrossref x where x.playlistId = :playlistId")
    fun getMaxPositionInPlaylist(playlistId: Long): Long
}