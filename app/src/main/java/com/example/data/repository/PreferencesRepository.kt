package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppThemeMode
import com.example.data.model.UnitSystem
import com.example.data.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("pulsetrack_user_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        val username = prefs.getString(KEY_USERNAME, "Athlete") ?: "Athlete"
        val weight = prefs.getFloat(KEY_WEIGHT, 70f)
        val unitStr = prefs.getString(KEY_UNIT, UnitSystem.METRIC.name) ?: UnitSystem.METRIC.name
        val themeStr = prefs.getString(KEY_THEME, AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        val autoPause = prefs.getBoolean(KEY_AUTO_PAUSE, true)
        val voice = prefs.getBoolean(KEY_VOICE, true)
        val awake = prefs.getBoolean(KEY_AWAKE, true)
        val accuracy = prefs.getFloat(KEY_ACCURACY, 30f)

        return UserSettings(
            username = username,
            weightKg = weight,
            unitSystem = try { UnitSystem.valueOf(unitStr) } catch (_: Exception) { UnitSystem.METRIC },
            themeMode = try { AppThemeMode.valueOf(themeStr) } catch (_: Exception) { AppThemeMode.DARK },
            autoPauseEnabled = autoPause,
            voiceAnnouncementsEnabled = voice,
            keepScreenAwake = awake,
            minAccuracyThresholdMeters = accuracy
        )
    }

    fun updateSettings(newSettings: UserSettings) {
        prefs.edit().apply {
            putString(KEY_USERNAME, newSettings.username)
            putFloat(KEY_WEIGHT, newSettings.weightKg)
            putString(KEY_UNIT, newSettings.unitSystem.name)
            putString(KEY_THEME, newSettings.themeMode.name)
            putBoolean(KEY_AUTO_PAUSE, newSettings.autoPauseEnabled)
            putBoolean(KEY_VOICE, newSettings.voiceAnnouncementsEnabled)
            putBoolean(KEY_AWAKE, newSettings.keepScreenAwake)
            putFloat(KEY_ACCURACY, newSettings.minAccuracyThresholdMeters)
            apply()
        }
        _settings.value = newSettings
    }

    companion object {
        private const val KEY_USERNAME = "pref_username"
        private const val KEY_WEIGHT = "pref_weight"
        private const val KEY_UNIT = "pref_unit"
        private const val KEY_THEME = "pref_theme"
        private const val KEY_AUTO_PAUSE = "pref_auto_pause"
        private const val KEY_VOICE = "pref_voice"
        private const val KEY_AWAKE = "pref_awake"
        private const val KEY_ACCURACY = "pref_accuracy"
    }
}
