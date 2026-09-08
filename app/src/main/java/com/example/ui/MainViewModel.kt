package com.example.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PulseTrackApp
import com.example.data.db.ActivityEntity
import com.example.data.db.ActivityWithPoints
import com.example.data.model.ActivityType
import com.example.data.model.UnitSystem
import com.example.data.model.UserSettings
import com.example.service.TrackingManager
import com.example.service.TrackingService
import com.example.service.TrackingState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val activityRepo = (application as PulseTrackApp).activityRepository
    private val prefRepo = (application as PulseTrackApp).preferencesRepository

    val allActivities: StateFlow<List<ActivityEntity>> = activityRepo.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettings> = prefRepo.settings

    val trackingState: StateFlow<TrackingState> = TrackingManager.state

    fun getActivityWithPoints(id: Long): StateFlow<ActivityWithPoints?> {
        return activityRepo.getActivityWithPoints(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun startTracking(
        activityType: ActivityType,
        targetDistanceKm: Double? = null,
        targetDurationMinutes: Int? = null,
        autoPause: Boolean = true,
        voice: Boolean = true
    ) {
        val targetDistanceMeters = targetDistanceKm?.let { it * 1000.0 }
        val targetDurationSeconds = targetDurationMinutes?.let { it * 60L }

        TrackingManager.startTracking(
            activityType = activityType,
            targetDistanceMeters = targetDistanceMeters,
            targetDurationSeconds = targetDurationSeconds,
            autoPauseEnabled = autoPause,
            voiceEnabled = voice
        )

        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    fun pauseTracking() {
        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            action = TrackingService.ACTION_PAUSE
        }
        getApplication<Application>().startService(intent)
    }

    fun resumeTracking() {
        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            action = TrackingService.ACTION_RESUME
        }
        getApplication<Application>().startService(intent)
    }

    fun finishTracking(title: String? = null, notes: String = "", onFinished: (Long) -> Unit) {
        viewModelScope.launch {
            val insertedId = TrackingManager.finishTracking(title, notes)
            val intent = Intent(getApplication(), TrackingService::class.java).apply {
                action = TrackingService.ACTION_STOP
            }
            getApplication<Application>().startService(intent)
            onFinished(insertedId)
        }
    }

    fun discardTracking() {
        TrackingManager.discardTracking()
        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }

    fun deleteActivity(id: Long) {
        viewModelScope.launch {
            activityRepo.deleteActivity(id)
        }
    }

    fun updateActivity(activity: ActivityEntity) {
        viewModelScope.launch {
            activityRepo.updateActivity(activity)
        }
    }

    fun updateSettings(newSettings: UserSettings) {
        prefRepo.updateSettings(newSettings)
    }

    fun clearAllData() {
        viewModelScope.launch {
            activityRepo.clearAll()
        }
    }

    // --- Stats Helpers ---

    fun formatDistance(distanceMeters: Double, unitSystem: UnitSystem): Pair<String, String> {
        return if (unitSystem == UnitSystem.METRIC) {
            val km = distanceMeters / 1000.0
            Pair("%.2f".format(km), "km")
        } else {
            val miles = (distanceMeters / 1000.0) * 0.621371
            Pair("%.2f".format(miles), "mi")
        }
    }

    fun formatSpeed(speedKmh: Double, unitSystem: UnitSystem): Pair<String, String> {
        return if (unitSystem == UnitSystem.METRIC) {
            Pair("%.1f".format(speedKmh), "km/h")
        } else {
            val mph = speedKmh * 0.621371
            Pair("%.1f".format(mph), "mph")
        }
    }

    fun formatPace(paceSecPerKm: Double, unitSystem: UnitSystem): Pair<String, String> {
        if (paceSecPerKm <= 0.0 || paceSecPerKm > 3600.0) {
            return Pair("--:--", if (unitSystem == UnitSystem.METRIC) "min/km" else "min/mi")
        }

        val effectivePaceSec = if (unitSystem == UnitSystem.METRIC) {
            paceSecPerKm
        } else {
            paceSecPerKm / 0.621371
        }

        val min = (effectivePaceSec / 60).toInt()
        val sec = (effectivePaceSec % 60).toInt()
        val paceStr = "%d:%02d".format(min, sec)
        val unitStr = if (unitSystem == UnitSystem.METRIC) "min/km" else "min/mi"
        return Pair(paceStr, unitStr)
    }

    fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            "%d:%02d:%02d".format(hrs, mins, secs)
        } else {
            "%02d:%02d".format(mins, secs)
        }
    }

    fun formatElevation(elevationMeters: Double, unitSystem: UnitSystem): Pair<String, String> {
        return if (unitSystem == UnitSystem.METRIC) {
            Pair("${elevationMeters.toInt()}", "m")
        } else {
            val feet = elevationMeters * 3.28084
            Pair("${feet.toInt()}", "ft")
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // Weekly Distances (Mon - Sun) for bar chart
    fun getWeeklyDistances(activities: List<ActivityEntity>): List<Pair<String, Double>> {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val calendar = Calendar.getInstance()
        // Find start of current week (Monday)
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis

        val dayDistances = MutableList(7) { 0.0 }

        activities.forEach { act ->
            if (act.startTimestamp >= weekStart) {
                val diffDays = ((act.startTimestamp - weekStart) / (24 * 3600 * 1000)).toInt()
                if (diffDays in 0..6) {
                    dayDistances[diffDays] += (act.distanceMeters / 1000.0)
                }
            }
        }

        return days.mapIndexed { index, name -> Pair(name, dayDistances[index]) }
    }

    // Current streak (consecutive days with at least 1 activity)
    fun calculateStreak(activities: List<ActivityEntity>): Int {
        if (activities.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        val activityDays = activities.map {
            calendar.timeInMillis = it.startTimestamp
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
        }.toSet()

        var streak = 0
        val checkCalendar = Calendar.getInstance()

        // Check today or yesterday
        val todayKey = "${checkCalendar.get(Calendar.YEAR)}-${checkCalendar.get(Calendar.DAY_OF_YEAR)}"
        checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayKey = "${checkCalendar.get(Calendar.YEAR)}-${checkCalendar.get(Calendar.DAY_OF_YEAR)}"

        var currentDay = if (activityDays.contains(todayKey)) {
            Calendar.getInstance()
        } else if (activityDays.contains(yesterdayKey)) {
            checkCalendar
        } else {
            return 0
        }

        while (true) {
            val key = "${currentDay.get(Calendar.YEAR)}-${currentDay.get(Calendar.DAY_OF_YEAR)}"
            if (activityDays.contains(key)) {
                streak++
                currentDay.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }
}
