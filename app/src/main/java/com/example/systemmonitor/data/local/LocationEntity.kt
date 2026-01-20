package com.example.systemmonitor.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Timestamp

@Entity(tableName = "location_table")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,
    @ColumnInfo("latitude") val latitude : Double,
    @ColumnInfo("longitude") val longitude  : Double,
    @ColumnInfo("timestamp")val timestamp: Long = System.currentTimeMillis()
)