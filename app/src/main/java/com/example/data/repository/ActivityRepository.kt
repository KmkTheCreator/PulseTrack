package com.example.data.repository

import com.example.data.db.ActivityDao
import com.example.data.db.ActivityEntity
import com.example.data.db.ActivityWithPoints
import com.example.data.db.GpsPointEntity
import kotlinx.coroutines.flow.Flow

class ActivityRepository(private val dao: ActivityDao) {

    val allActivities: Flow<List<ActivityEntity>> = dao.getAllActivities()

    fun getActivityWithPoints(id: Long): Flow<ActivityWithPoints?> = dao.getActivityWithPoints(id)

    fun getActivityById(id: Long): Flow<ActivityEntity?> = dao.getActivityById(id)

    fun getPointsForActivity(activityId: Long): Flow<List<GpsPointEntity>> = dao.getPointsForActivity(activityId)

    suspend fun saveActivityWithPoints(activity: ActivityEntity, points: List<GpsPointEntity>): Long {
        val activityId = dao.insertActivity(activity)
        if (points.isNotEmpty()) {
            val linkedPoints = points.map { it.copy(activityId = activityId) }
            dao.insertGpsPoints(linkedPoints)
        }
        return activityId
    }

    suspend fun updateActivity(activity: ActivityEntity) = dao.updateActivity(activity)

    suspend fun deleteActivity(id: Long) = dao.deleteActivityById(id)

    suspend fun clearAll() = dao.deleteAllActivities()
}
