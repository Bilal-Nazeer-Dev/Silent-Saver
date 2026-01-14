package com.example.silentsaver.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations ORDER BY id DESC")
    fun getAllLocations(): Flow<List<LocationEntity>> // Flow updates the UI automatically!

    @Insert
    suspend fun insert(location: LocationEntity)

    @Delete
    suspend fun delete(location: LocationEntity)

    @Update
    suspend fun update(location: LocationEntity)
}