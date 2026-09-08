package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val title: String,
    val notes: String = "",
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationSeconds: Long,
    val movingTimeSeconds: Long,
    val stoppedTimeSeconds: Long = 0,
    val distanceMeters: Double,
    val avgPaceSecPerKm: Double,
    val fastestPaceSecPerKm: Double = 0.0,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0,
    val calories: Int = 0
)
