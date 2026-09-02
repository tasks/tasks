package org.tasks.data.dao

import androidx.room.Dao
import androidx.room.Query
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData

@Dao
interface ApiDao {
    @Query("SELECT * FROM alarms WHERE _id = :id")
    suspend fun getAlarm(id: Long): Alarm?

    @Query("SELECT * FROM geofences WHERE geofence_id = :id")
    suspend fun getGeofence(id: Long): Geofence?

    @Query("SELECT * FROM tags WHERE _id = :id")
    suspend fun getTaskTag(id: Long): Tag?

    @Query("SELECT * FROM tagdata WHERE _id = :id")
    suspend fun getTag(id: Long): TagData?
}
