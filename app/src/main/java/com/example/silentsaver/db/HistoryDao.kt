package com.example.silentsaver.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    // Get all logs, newest first
    @Query("SELECT * FROM history_logs ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    // Get count of events in the last 7 days (Optional helper)
    @Query("SELECT COUNT(*) FROM history_logs WHERE timestamp > :sevenDaysAgo")
    suspend fun getWeeklyCount(sevenDaysAgo: Long): Int

    @Insert
    suspend fun insert(log: HistoryEntity)

    // --- UPDATED: Renamed to 'deleteAll' to match HistoryActivity ---
    @Query("DELETE FROM history_logs")
    suspend fun deleteAll()
}