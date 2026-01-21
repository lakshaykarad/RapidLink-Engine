package com.example.systemmonitor.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Query("SELECT * FROM location_table ORDER BY timestamp ASC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("DELETE FROM location_table")
    suspend fun clearAllLocations()

}