package com.example.silentsaver.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_logs")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,       // e.g., "Entered Mosque"
    val timestamp: Long,     // Time in milliseconds
    val duration: String,    // e.g., "Active" or "30 mins"
    val type: String         // "GEOFENCE", "SCHEDULE", "TIMER"
)