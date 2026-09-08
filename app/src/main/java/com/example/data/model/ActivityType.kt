package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.ui.graphics.vector.ImageVector

enum class ActivityType(
    val displayName: String,
    val metValue: Double,
    val defaultTitle: String
) {
    RUNNING("Running", 9.8, "Morning Run"),
    WALKING("Walking", 3.8, "Outdoor Walk"),
    CYCLING("Cycling", 7.5, "Road Cycling"),
    HIKING("Hiking", 6.0, "Trail Hike");

    val icon: ImageVector
        get() = when (this) {
            RUNNING -> Icons.Default.DirectionsRun
            WALKING -> Icons.Default.DirectionsWalk
            CYCLING -> Icons.Default.DirectionsBike
            HIKING -> Icons.Default.Terrain
        }

    companion object {
        fun fromString(type: String): ActivityType {
            return entries.find { it.name.equals(type, ignoreCase = true) } ?: RUNNING
        }
    }
}
