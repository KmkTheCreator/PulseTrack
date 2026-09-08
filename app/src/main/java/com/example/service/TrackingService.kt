package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.PulseTrackApp
import com.example.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var stateObserverJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        TrackingManager.initialize(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    TrackingManager.onLocationUpdate(location)
                }
            }
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PulseTrack::TrackingWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundTracking()
            }
            ACTION_PAUSE -> {
                TrackingManager.pauseTracking()
            }
            ACTION_RESUME -> {
                TrackingManager.resumeTracking()
            }
            ACTION_STOP -> {
                stopTrackingService()
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startForegroundTracking() {
        wakeLock?.let {
            if (!it.isHeld) {
                it.acquire(4 * 60 * 60 * 1000L) // 4 hours maximum safety timeout
            }
        }

        val notification = buildNotification(TrackingManager.state.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startLocationUpdates()
        observeStateForNotificationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(2f)
                .setWaitForAccurateLocation(false)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun observeStateForNotificationUpdates() {
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            TrackingManager.state.collectLatest { state ->
                if (!state.isTracking) {
                    stopTrackingService()
                } else {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    manager?.notify(NOTIFICATION_ID, buildNotification(state))
                }
            }
        }
    }

    private fun buildNotification(state: TrackingState): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distKm = "%.2f".format(state.distanceMeters / 1000.0)
        val paceMin = (state.avgPaceSecPerKm / 60).toInt()
        val paceSec = (state.avgPaceSecPerKm % 60).toInt()
        val paceFormatted = if (state.avgPaceSecPerKm > 0) "%d:%02d/km".format(paceMin, paceSec) else "--:--/km"
        val timeFormatted = formatDuration(state.elapsedSeconds)

        val statusText = when {
            state.isPaused -> "PAUSED"
            state.isAutoPaused -> "AUTO-PAUSED"
            else -> "${state.activityType.displayName} in progress"
        }

        val contentText = "$distKm km • $paceFormatted • $timeFormatted"

        val builder = NotificationCompat.Builder(this, PulseTrackApp.CHANNEL_TRACKING_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("PulseTrack • $statusText")
            .setContentText(contentText)
            .setContentIntent(pendingOpenIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        // Add Pause/Resume Action
        if (state.isPaused) {
            val resumeIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_RESUME }
            val pendingResume = PendingIntent.getService(
                this, 1, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume", pendingResume)
        } else {
            val pauseIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_PAUSE }
            val pendingPause = PendingIntent.getService(
                this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pendingPause)
        }

        // Add Stop Action
        val stopIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_STOP }
        val pendingStop = PendingIntent.getService(
            this, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStop)

        return builder.build()
    }

    private fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            "%d:%02d:%02d".format(hrs, mins, secs)
        } else {
            "%02d:%02d".format(mins, secs)
        }
    }

    private fun stopTrackingService() {
        stateObserverJob?.cancel()
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTrackingService()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.action.START_TRACKING"
        const val ACTION_PAUSE = "com.example.action.PAUSE_TRACKING"
        const val ACTION_RESUME = "com.example.action.RESUME_TRACKING"
        const val ACTION_STOP = "com.example.action.STOP_TRACKING"
        private const val NOTIFICATION_ID = 9001
    }
}
