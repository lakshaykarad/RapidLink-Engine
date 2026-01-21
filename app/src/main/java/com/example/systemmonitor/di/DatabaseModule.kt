package com.example.systemmonitor.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.systemmonitor.data.local.LocationDao
import com.example.systemmonitor.data.local.RapidDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.internal.Contexts
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context : Context) : RapidDatabase{
        return Room.databaseBuilder(
            context,
            RapidDatabase::class.java,
            "rapid_link.db"
        ).build()
    }

    @Provides
    fun provideLocationDao(database: RapidDatabase) : LocationDao{
        return database.locationDao()
    }

}