package org.tasks.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationDao {
    @Query("SELECT task FROM notification")
    suspend fun getAll(): List<Long>

    @Query("SELECT * FROM notification ORDER BY timestamp DESC")
    suspend fun getAllOrdered(): List<Notification>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<Notification>)

    @Query("DELETE FROM notification WHERE task = :taskId")
    suspend fun delete(taskId: Long)

    @Query("DELETE FROM notification WHERE task IN(:taskIds)")
    suspend fun deleteAll(taskIds: List<Long>)

    @Query("SELECT MAX(timestamp) FROM notification")
    suspend fun latestTimestamp(): Long?
}