package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {
    @Query("SELECT * FROM recent_files ORDER BY timestamp DESC")
    fun getAllRecentFiles(): Flow<List<RecentFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(recentFile: RecentFile)

    @Query("DELETE FROM recent_files WHERE id = :id")
    suspend fun deleteRecentFileById(id: Int)

    @Query("DELETE FROM recent_files WHERE uriString = :uriString")
    suspend fun deleteRecentFileByUri(uriString: String)

    @Query("DELETE FROM recent_files")
    suspend fun deleteAllRecentFiles()
}
