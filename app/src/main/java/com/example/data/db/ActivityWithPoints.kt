package com.example.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class ActivityWithPoints(
    @Embedded val activity: ActivityEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "activityId"
    )
    val points: List<GpsPointEntity>
)
