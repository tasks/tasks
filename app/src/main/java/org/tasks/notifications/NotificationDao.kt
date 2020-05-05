package org.tasks.notifications

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationDao {
    @Query("SELECT task FROM notification")
    fun getAll(): List<Long>

    @Query("SELECT * FROM notification ORDER BY timestamp DESC")
    fun getAllOrdered(): List<Notification>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(notifications: List<Notification>)

    @Query("DELETE FROM notification WHERE task = :taskId")
    fun delete(taskId: Long)

    @Query("DELETE FROM notification WHERE task IN(:taskIds)")
    fun deleteAll(taskIds: List<Long>)

    @Query("SELECT MAX(timestamp) FROM notification")
    fun latestTimestamp(): Long
}