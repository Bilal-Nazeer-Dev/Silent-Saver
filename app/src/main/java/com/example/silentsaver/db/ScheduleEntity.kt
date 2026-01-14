package com.example.silentsaver.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val timeRange: String,
    val days: String,
    val isActive: Boolean,
    // We save raw time values so we can reset alarms if needed later
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
)