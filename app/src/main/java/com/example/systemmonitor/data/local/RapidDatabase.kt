package com.example.systemmonitor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LocationEntity::class], version = 1, exportSchema = false)
abstract class RapidDatabase : RoomDatabase(){
    abstract fun locationDao() : LocationDao
}