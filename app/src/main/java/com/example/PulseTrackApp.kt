package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.db.PulseTrackDatabase
import com.example.data.repository.ActivityRepository
import com.example.data.repository.PreferencesRepository
import org.osmdroid.config.Configuration

class PulseTrackApp : Application() {

    lateinit var database: PulseTrackDatabase
        private set

    lateinit var activityRepository: ActivityRepository
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize OSMDroid
        try {
            Configuration.getInstance().load(this, getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE))
            Configuration.getInstance().userAgentValue = packageName
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize Database & Repositories
        database = PulseTrackDatabase.getDatabase(this)
        activityRepository = ActivityRepository(database.activityDao())
        preferencesRepository = PreferencesRepository(this)

        // Create Notification Channel for Tracking
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_TRACKING_ID,
                "PulseTrack Active Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time live activity stats while recording"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_TRACKING_ID = "pulsetrack_tracking_channel"
        lateinit var instance: PulseTrackApp
            private set
    }
}
