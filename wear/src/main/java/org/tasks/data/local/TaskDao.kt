/**
 * TaskDao.kt — Room DAO for the `tasks` table.
 *
 * ## Query groups
 * | Group                | Methods                                       | Used by                |
 * |----------------------|-----------------------------------------------|------------------------|
 * | **Read (UI)**        | getTasks, getTasksOnce, getTask, getTaskFlow  | TaskRepository → UI    |
 * | **Write (local)**    | insert, update, softDelete, setCompleted, …    | SyncRepository         |
 * | **Conflict resolve** | updateTitleIfNewer, updateNotesIfNewer, …      | SyncRepository (inbox) |
 * | **Sync helpers**     | getDirtyTasks, markSynced, getTaskByPhoneId   | SyncRepository         |
 * | **Cleanup**          | cleanupDeletedTasks, hardDelete                | SyncWorker             |
 * | **Notifications**    | getTasksWithReminders                          | WearNotificationMgr   |
 *
 * All write methods except the `IfNewer` variants set `dirty = 1` so the
 * outbox knows there are pending local changes.
 */
package org.tasks.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TaskEntity.
 * Provides methods for accessing the tasks table.
 */
@Dao
interface TaskDao {

    /**
     * Get all non-deleted tasks ordered by updatedAt descending.
     * Returns a Flow for reactive updates.
     */
    @Query("SELECT * FROM tasks WHERE deleted = 0 ORDER BY updatedAt DESC")
    fun getTasks(): Flow<List<TaskEntity>>

    /**
     * Get all non-deleted tasks (non-reactive, for one-shot reads).
     */
    @Query("SELECT * FROM tasks WHERE deleted = 0 ORDER BY updatedAt DESC")
    suspend fun getTasksOnce(): List<TaskEntity>

    /**
     * Get a single task by ID.
     */
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTask(id: String): TaskEntity?

    /**
     * Get a single task by ID as Flow for reactive updates.
     */
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskFlow(id: String): Flow<TaskEntity?>

    /**
     * Insert a new task. If conflict, replace the existing one.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    /**
     * Insert multiple tasks. If conflict, replace existing ones.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    /**
     * Update an existing task.
     */
    @Update
    suspend fun update(task: TaskEntity)

    /**
     * Soft delete a task (set deleted = true).
     */
    @Query("UPDATE tasks SET deleted = 1, updatedAt = :timestamp, dirty = 1 WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Hard delete a task (remove from database).
     */
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * Mark a task as completed or not.
     */
    @Query("UPDATE tasks SET completed = :completed, completedUpdatedAt = :timestamp, updatedAt = :timestamp, dirty = 1 WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean, timestamp: Long = System.currentTimeMillis())

    /**
     * Update task title and notes with per-field timestamps.
     */
    @Query("UPDATE tasks SET title = :title, titleUpdatedAt = :timestamp, notes = :notes, notesUpdatedAt = :timestamp, updatedAt = :timestamp, dirty = 1 WHERE id = :id")
    suspend fun updateTitleAndNotes(id: String, title: String, notes: String?, timestamp: Long = System.currentTimeMillis())

    /**
     * Update only the title with its timestamp.
     */
    @Query("UPDATE tasks SET title = :title, titleUpdatedAt = :timestamp, updatedAt = :timestamp, dirty = 1 WHERE id = :id")
    suspend fun updateTitle(id: String, title: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Update only the notes with its timestamp.
     */
    @Query("UPDATE tasks SET notes = :notes, notesUpdatedAt = :timestamp, updatedAt = :timestamp, dirty = 1 WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String?, timestamp: Long = System.currentTimeMillis())

    /**
     * Get count of non-deleted tasks.
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE deleted = 0")
    suspend fun getTaskCount(): Int

    // ===== Sync-related queries =====

    /**
     * Get all dirty tasks (local changes not yet synced).
     */
    @Query("SELECT * FROM tasks WHERE dirty = 1")
    suspend fun getDirtyTasks(): List<TaskEntity>

    /**
     * Mark a task as synced (clean).
     */
    @Query("UPDATE tasks SET dirty = 0, syncedAt = :timestamp WHERE id = :id")
    suspend fun markSynced(id: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Mark multiple tasks as synced.
     */
    @Query("UPDATE tasks SET dirty = 0, syncedAt = :timestamp WHERE id IN (:ids)")
    suspend fun markSyncedBatch(ids: List<String>, timestamp: Long = System.currentTimeMillis())

    /**
     * Update title only if the incoming timestamp is newer (for conflict resolution).
     */
    @Query("UPDATE tasks SET title = :title, titleUpdatedAt = :timestamp WHERE id = :id AND titleUpdatedAt < :timestamp")
    suspend fun updateTitleIfNewer(id: String, title: String, timestamp: Long): Int

    /**
     * Update notes only if the incoming timestamp is newer (for conflict resolution).
     */
    @Query("UPDATE tasks SET notes = :notes, notesUpdatedAt = :timestamp WHERE id = :id AND notesUpdatedAt < :timestamp")
    suspend fun updateNotesIfNewer(id: String, notes: String?, timestamp: Long): Int

    /**
     * Update completed only if the incoming timestamp is newer (for conflict resolution).
     */
    @Query("UPDATE tasks SET completed = :completed, completedUpdatedAt = :timestamp WHERE id = :id AND completedUpdatedAt < :timestamp")
    suspend fun updateCompletedIfNewer(id: String, completed: Boolean, timestamp: Long): Int

    /**
     * Get all tasks including deleted ones (for sync).
     */
    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    suspend fun getAllTasksForSync(): List<TaskEntity>

    /**
     * Get tasks modified after a certain timestamp (for incremental sync).
     */
    @Query("SELECT * FROM tasks WHERE updatedAt > :since ORDER BY updatedAt ASC")
    suspend fun getTasksModifiedAfter(since: Long): List<TaskEntity>

    /**
     * Hard delete tasks that are deleted and synced (cleanup).
     */
    @Query("DELETE FROM tasks WHERE deleted = 1 AND dirty = 0 AND syncedAt > 0 AND syncedAt < :threshold")
    suspend fun cleanupDeletedTasks(threshold: Long)

    /**
     * Find a task by its phone task ID (for sync mapping).
     */
    @Query("SELECT * FROM tasks WHERE phoneTaskId = :phoneId LIMIT 1")
    suspend fun getTaskByPhoneId(phoneId: Long): TaskEntity?

    /**
     * Find dirty tasks matching a title (for duplicate detection during initial sync).
     * This helps match locally created tasks with their phone counterparts.
     */
    @Query("SELECT * FROM tasks WHERE title = :title AND dirty = 1 AND phoneTaskId IS NULL LIMIT 1")
    suspend fun findDirtyTaskByTitle(title: String): TaskEntity?

    /**
     * Hard delete a task by ID (used when phone reports deletion).
     */
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun hardDelete(id: String)

    /**
     * Hard delete a task by phone ID (used when phone reports deletion).
     */
    @Query("DELETE FROM tasks WHERE phoneTaskId = :phoneId")
    suspend fun hardDeleteByPhoneId(phoneId: Long)

    /**
     * Update the phoneTaskId for a task.
     */
    @Query("UPDATE tasks SET phoneTaskId = :phoneId WHERE id = :id")
    suspend fun setPhoneTaskId(id: String, phoneId: Long)

    /**
     * Update due date, time and reminder for a task.
     */
    @Query("UPDATE tasks SET dueDate = :dueDate, dueTime = :dueTime, reminder = :reminder, reminderTime = :reminderTime, updatedAt = :timestamp, dirty = 1 WHERE id = :id")
    suspend fun updateDueDateAndReminder(
        id: String,
        dueDate: Long?,
        dueTime: Long?,
        reminder: Boolean,
        reminderTime: Long?,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Get all tasks that have reminders enabled and are not completed/deleted.
     */
    @Query("SELECT * FROM tasks WHERE reminder = 1 AND completed = 0 AND deleted = 0")
    suspend fun getTasksWithReminders(): List<TaskEntity>
}

