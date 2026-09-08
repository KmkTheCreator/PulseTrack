package com.example.data.model

enum class UnitSystem(val displayName: String, val distanceUnit: String, val paceUnit: String, val speedUnit: String, val elevationUnit: String) {
    METRIC("Metric (km, m)", "km", "min/km", "km/h", "m"),
    IMPERIAL("Imperial (mi, ft)", "mi", "min/mi", "mph", "ft")
}

enum class AppThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    DARK("Dark Theme"),
    LIGHT("Light Theme")
}

data class UserSettings(
    val username: String = "Athlete",
    val weightKg: Float = 70f,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val autoPauseEnabled: Boolean = true,
    val voiceAnnouncementsEnabled: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val minAccuracyThresholdMeters: Float = 30f
)
