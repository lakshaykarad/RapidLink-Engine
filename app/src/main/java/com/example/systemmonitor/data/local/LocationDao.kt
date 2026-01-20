package com.example.systemmonitor.data.local

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(locationDao: LocationDao) : Long

    @Query("SELECT * FROM location_table ORDER BY timestamp ASC")
    suspend fun getAllLocation(locationDao: LocationDao) : Flow<List<LocationEntity>>

    @Query("DELETE FROM location_table")
    suspend fun clearAllLocation()
}