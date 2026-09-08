package com.example.service

import android.content.Context
import android.location.Location
import com.example.PulseTrackApp
import com.example.data.db.ActivityEntity
import com.example.data.db.GpsPointEntity
import com.example.data.model.ActivityType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

object TrackingManager {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(TrackingState())
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    private var lastValidLocation: Location? = null
    private var lastAltitude: Double? = null
    private var lastSplitKilometer: Int = 0
    private var consecutiveLowSpeedCount = 0

    private var ttsHelper: TextToSpeechHelper? = null

    fun initialize(context: Context) {
        if (ttsHelper == null) {
            ttsHelper = TextToSpeechHelper(context.applicationContext)
        }
    }

    fun startTracking(
        activityType: ActivityType,
        targetDistanceMeters: Double? = null,
        targetDurationSeconds: Long? = null,
        autoPauseEnabled: Boolean = true,
        voiceEnabled: Boolean = true
    ) {
        lastValidLocation = null
        lastAltitude = null
        lastSplitKilometer = 0
        consecutiveLowSpeedCount = 0

        val now = System.currentTimeMillis()
        _state.value = TrackingState(
            isTracking = true,
            isPaused = false,
            isAutoPaused = false,
            activityType = activityType,
            targetDistanceMeters = targetDistanceMeters,
            targetDurationSeconds = targetDurationSeconds,
            autoPauseEnabled = autoPauseEnabled,
            voiceEnabled = voiceEnabled,
            startTimestamp = now,
            elapsedSeconds = 0L,
            movingSeconds = 0L,
            stoppedSeconds = 0L,
            distanceMeters = 0.0,
            points = emptyList(),
            isFinished = false,
            finishedActivityId = null
        )

        if (voiceEnabled) {
            ttsHelper?.speak("Starting ${activityType.displayName} activity.")
        }

        startTimer()
    }

    fun pauseTracking() {
        if (!_state.value.isTracking || _state.value.isPaused) return
        _state.update { it.copy(isPaused = true, isAutoPaused = false) }
        if (_state.value.voiceEnabled) {
            ttsHelper?.speak("Activity paused.")
        }
    }

    fun resumeTracking() {
        if (!_state.value.isTracking || !_state.value.isPaused) return
        _state.update { it.copy(isPaused = false, isAutoPaused = false) }
        if (_state.value.voiceEnabled) {
            ttsHelper?.speak("Activity resumed.")
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _state.value.isTracking) {
                delay(1000L)
                val current = _state.value
                if (!current.isTracking) break

                val isPausedOrAutoPaused = current.isPaused || current.isAutoPaused

                val newElapsed = current.elapsedSeconds + 1
                val newMoving = if (!isPausedOrAutoPaused) current.movingSeconds + 1 else current.movingSeconds
                val newStopped = if (isPausedOrAutoPaused) current.stoppedSeconds + 1 else current.stoppedSeconds

                // Recalculate average pace and speed
                val distKm = current.distanceMeters / 1000.0
                val avgPaceSec = if (distKm > 0.05 && newMoving > 0) {
                    newMoving / distKm
                } else 0.0

                val avgSpeed = if (newMoving > 0) {
                    (current.distanceMeters / newMoving) * 3.6
                } else 0.0

                // Calorie estimate: MET * 3.5 * weightKg (approx 70) / 200 * (time in minutes)
                val movingMinutes = newMoving / 60.0
                val calories = (current.activityType.metValue * 3.5 * 70.0 / 200.0 * movingMinutes).roundToInt()

                _state.update {
                    it.copy(
                        elapsedSeconds = newElapsed,
                        movingSeconds = newMoving,
                        stoppedSeconds = newStopped,
                        avgPaceSecPerKm = avgPaceSec,
                        avgSpeedKmh = avgSpeed,
                        calories = calories
                    )
                }
            }
        }
    }

    fun onLocationUpdate(location: Location) {
        val current = _state.value
        if (!current.isTracking) {
            // Still update live GPS accuracy and coordinates for setup screen
            _state.update {
                it.copy(
                    gpsAccuracyMeters = location.accuracy,
                    currentLatitude = location.latitude,
                    currentLongitude = location.longitude,
                    currentAltitude = if (location.hasAltitude()) location.altitude else null
                )
            }
            return
        }

        // Drop points with poor accuracy (> 35m)
        if (location.accuracy > 35f) {
            _state.update { it.copy(gpsAccuracyMeters = location.accuracy) }
            return
        }

        // Speed calculation
        var speedMs = if (location.hasSpeed()) location.speed else 0f
        val prevLoc = lastValidLocation

        if (prevLoc != null && speedMs <= 0f) {
            val dist = prevLoc.distanceTo(location)
            val timeDiffSec = (location.time - prevLoc.time) / 1000f
            if (timeDiffSec > 0.5f) {
                speedMs = dist / timeDiffSec
            }
        }

        // Filter unrealistic GPS jump speeds (e.g. > 45 m/s or 162 km/h)
        if (speedMs > 45f) {
            return
        }

        val speedKmh = speedMs * 3.6

        // Check movement threshold
        val isMoving = speedMs >= 0.7f // ~2.5 km/h

        // Auto-pause handling
        if (current.autoPauseEnabled && !current.isPaused) {
            if (!isMoving) {
                consecutiveLowSpeedCount++
                if (consecutiveLowSpeedCount >= 3 && !current.isAutoPaused) {
                    _state.update { it.copy(isAutoPaused = true) }
                    if (current.voiceEnabled) {
                        ttsHelper?.speak("Auto paused.")
                    }
                }
            } else {
                consecutiveLowSpeedCount = 0
                if (current.isAutoPaused) {
                    _state.update { it.copy(isAutoPaused = false) }
                    if (current.voiceEnabled) {
                        ttsHelper?.speak("Auto resumed.")
                    }
                }
            }
        }

        val effectivelyPaused = current.isPaused || current.isAutoPaused

        var addedDistance = 0.0
        var addedElevationGain = 0.0
        var addedElevationLoss = 0.0

        if (!effectivelyPaused && prevLoc != null) {
            val d = prevLoc.distanceTo(location).toDouble()
            // Ignore micro-jitter if accuracy is loose
            if (d >= 1.5) {
                addedDistance = d
            }

            // Elevation filtering with hysteresis: at least 2.5m delta to reduce barometer/GPS noise
            if (location.hasAltitude()) {
                val lastAlt = lastAltitude
                if (lastAlt != null) {
                    val altDelta = location.altitude - lastAlt
                    if (altDelta > 2.5) {
                        addedElevationGain = altDelta
                        lastAltitude = location.altitude
                    } else if (altDelta < -2.5) {
                        addedElevationLoss = -altDelta
                        lastAltitude = location.altitude
                    }
                } else {
                    lastAltitude = location.altitude
                }
            }
        } else if (lastAltitude == null && location.hasAltitude()) {
            lastAltitude = location.altitude
        }

        val newTotalDistance = current.distanceMeters + addedDistance
        val newElevationGain = current.elevationGainMeters + addedElevationGain
        val newElevationLoss = current.elevationLossMeters + addedElevationLoss

        // Current pace (min/km in seconds)
        val currentPace = if (speedMs > 0.5f) {
            1000.0 / speedMs
        } else 0.0

        val maxSpeed = maxOf(current.maxSpeedKmh, speedKmh)
        val fastestPace = if (currentPace > 0.0) {
            if (current.fastestPaceSecPerKm == 0.0) currentPace
            else minOf(current.fastestPaceSecPerKm, currentPace)
        } else current.fastestPaceSecPerKm

        // Create point entity
        val newPoint = GpsPointEntity(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = if (location.hasAltitude()) location.altitude else 0.0,
            speed = speedMs,
            accuracy = location.accuracy,
            timestamp = location.time,
            isMoving = isMoving
        )

        val updatedPoints = if (!effectivelyPaused) {
            current.points + newPoint
        } else {
            current.points
        }

        lastValidLocation = location

        _state.update {
            it.copy(
                distanceMeters = newTotalDistance,
                currentSpeedKmh = speedKmh,
                maxSpeedKmh = maxSpeed,
                currentPaceSecPerKm = currentPace,
                fastestPaceSecPerKm = fastestPace,
                elevationGainMeters = newElevationGain,
                elevationLossMeters = newElevationLoss,
                gpsAccuracyMeters = location.accuracy,
                currentLatitude = location.latitude,
                currentLongitude = location.longitude,
                currentAltitude = if (location.hasAltitude()) location.altitude else null,
                points = updatedPoints
            )
        }

        // Voice split announcements (every 1 km)
        if (current.voiceEnabled && !effectivelyPaused) {
            val currentKm = (newTotalDistance / 1000.0).toInt()
            if (currentKm > lastSplitKilometer && currentKm >= 1) {
                lastSplitKilometer = currentKm
                val paceMin = (currentPace / 60).toInt()
                val paceSec = (currentPace % 60).toInt()
                ttsHelper?.speak(
                    "Kilometer $currentKm. Pace: $paceMin minutes $paceSec seconds per kilometer."
                )
            }
        }
    }

    suspend fun finishTracking(title: String? = null, notes: String = ""): Long {
        timerJob?.cancel()
        val current = _state.value

        val endTime = System.currentTimeMillis()
        val finalTitle = title?.ifBlank { null } ?: "${current.activityType.defaultTitle}"

        val activityEntity = ActivityEntity(
            type = current.activityType.name,
            title = finalTitle,
            notes = notes,
            startTimestamp = current.startTimestamp,
            endTimestamp = endTime,
            durationSeconds = current.elapsedSeconds,
            movingTimeSeconds = current.movingSeconds,
            stoppedTimeSeconds = current.stoppedSeconds,
            distanceMeters = current.distanceMeters,
            avgPaceSecPerKm = current.avgPaceSecPerKm,
            fastestPaceSecPerKm = current.fastestPaceSecPerKm,
            avgSpeedKmh = current.avgSpeedKmh,
            maxSpeedKmh = current.maxSpeedKmh,
            elevationGainMeters = current.elevationGainMeters,
            elevationLossMeters = current.elevationLossMeters,
            calories = current.calories
        )

        val insertedId = PulseTrackApp.instance.activityRepository.saveActivityWithPoints(
            activityEntity,
            current.points
        )

        if (current.voiceEnabled) {
            val distKm = "%.2f".format(current.distanceMeters / 1000.0)
            ttsHelper?.speak("Activity finished. Great job! Total distance: $distKm kilometers.")
        }

        _state.update {
            it.copy(
                isTracking = false,
                isPaused = false,
                isAutoPaused = false,
                isFinished = true,
                finishedActivityId = insertedId
            )
        }

        return insertedId
    }

    fun discardTracking() {
        timerJob?.cancel()
        lastValidLocation = null
        lastAltitude = null
        _state.value = TrackingState()
    }

    fun resetFinishedState() {
        _state.value = TrackingState()
    }
}
