package org.tasks.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.tasks.data.db.SuspendDbUtils.eachChunk
import org.tasks.data.entity.Notification

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

    suspend fun deleteAll(taskIds: List<Long>) = taskIds.eachChunk { deleteAllInternal(it) }

    @Query("DELETE FROM notification WHERE task IN(:taskIds)")
    suspend fun deleteAllInternal(taskIds: List<Long>)

    @Query("SELECT MAX(timestamp) FROM notification")
    suspend fun latestTimestamp(): Long?

    @Query("SELECT EXISTS(SELECT 1 FROM notification WHERE task = :taskId)")
    suspend fun hasNotification(taskId: Long): Boolean

    @Query("SELECT * FROM notification WHERE task = :taskId")
    suspend fun get(taskId: Long): Notification?

    @Query("SELECT type FROM notification WHERE task = :taskId")
    suspend fun getType(taskId: Long): Int?
}
