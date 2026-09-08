package com.example.service

import com.example.data.db.GpsPointEntity
import com.example.data.model.ActivityType

data class TrackingState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val isAutoPaused: Boolean = false,
    val activityType: ActivityType = ActivityType.RUNNING,
    val targetDistanceMeters: Double? = null,
    val targetDurationSeconds: Long? = null,
    val autoPauseEnabled: Boolean = true,
    val voiceEnabled: Boolean = true,
    val startTimestamp: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val movingSeconds: Long = 0L,
    val stoppedSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val currentPaceSecPerKm: Double = 0.0,
    val avgPaceSecPerKm: Double = 0.0,
    val fastestPaceSecPerKm: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0,
    val calories: Int = 0,
    val gpsAccuracyMeters: Float = 0f,
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val currentAltitude: Double? = null,
    val points: List<GpsPointEntity> = emptyList(),
    val isFinished: Boolean = false,
    val finishedActivityId: Long? = null
)
