package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity): Long

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: Long)

    @Query("DELETE FROM activities")
    suspend fun deleteAllActivities()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpsPoints(points: List<GpsPointEntity>)

    @Query("SELECT * FROM activities ORDER BY startTimestamp DESC")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    fun getActivityById(id: Long): Flow<ActivityEntity?>

    @Query("SELECT * FROM gps_points WHERE activityId = :activityId ORDER BY timestamp ASC")
    fun getPointsForActivity(activityId: Long): Flow<List<GpsPointEntity>>

    @Transaction
    @Query("SELECT * FROM activities WHERE id = :id")
    fun getActivityWithPoints(id: Long): Flow<ActivityWithPoints?>
}
