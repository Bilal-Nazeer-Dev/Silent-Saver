package com.example.silentsaver.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// INCREASED VERSION TO 4, ADDED ScheduleEntity
@Database(entities = [LocationEntity::class, ContactEntity::class, HistoryEntity::class, ScheduleEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun contactDao(): ContactDao
    abstract fun historyDao(): HistoryDao
    abstract fun scheduleDao(): ScheduleDao // <--- ADDED THIS

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "silent_saver_db"
                )
                    .fallbackToDestructiveMigration() // Wipe data if version changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}