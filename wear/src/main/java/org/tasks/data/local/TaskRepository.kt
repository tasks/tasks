/**
 * TaskRepository.kt — Single point of access for task CRUD on Wear OS.
 *
 * ## Role in the architecture
 * ```
 * UI (ViewModel)
 *       │
 *       ▼
 *  TaskRepository  ──▶  SyncRepository  ──▶  OutboxOpDao  (queue for phone)
 *       │                    │
 *       ▼                    ▼
 *    TaskDao             WearDatabase
 * ```
 *
 * Every mutating method (save, toggle complete, delete) writes the change
 * **atomically** via [SyncRepository] so that a matching outbox operation
 * is always created inside the same Room transaction.
 *
 * Read methods return [TaskLite] (the UI model), converting from
 * [TaskEntity] via the private `toTaskLite()` extension.
 *
 * Also integrates with [WearNotificationManager] to schedule / cancel
 * reminder alarms whenever a task is saved or completed.
 *
 * ## Singleton
 * Accessed via [TaskRepository.getInstance].
 */
package org.tasks.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.tasks.data.sync.SyncRepository
import org.tasks.notifications.WearNotificationManager
import org.tasks.presentation.model.TaskLite
import java.util.UUID

/**
 * Repository that provides access to task data.
 * Abstracts the data source (Room) from the UI layer.
 * Operations that modify data use SyncRepository to queue sync operations.
 */
class TaskRepository(private val context: Context) {

    private val database = WearDatabase.getInstance(context)
    private val taskDao = database.taskDao()
    private val syncRepository = SyncRepository.getInstance(context)
    private val notificationManager = WearNotificationManager.getInstance(context)

    /**
     * Get all tasks as a Flow of TaskLite list.
     * The Flow will emit new values when the database changes.
     */
    fun getTasks(): Flow<List<TaskLite>> {
        return taskDao.getTasks().map { entities ->
            entities.map { it.toTaskLite() }
        }
    }

    /**
     * Get all tasks once (non-reactive).
     */
    suspend fun getTasksOnce(): List<TaskLite> {
        return taskDao.getTasksOnce().map { it.toTaskLite() }
    }

    /**
     * Get a single task by ID.
     */
    suspend fun getTask(id: String): TaskLite? {
        return taskDao.getTask(id)?.toTaskLite()
    }

    /**
     * Get a single task by Long ID (for backward compatibility).
     */
    suspend fun getTask(id: Long): TaskLite? {
        return getTask(id.toString())
    }

    /**
     * Get a task as Flow for reactive updates.
     */
    fun getTaskFlow(id: String): Flow<TaskLite?> {
        return taskDao.getTaskFlow(id).map { it?.toTaskLite() }
    }

    /**
     * Save a task (create or update) with sync support.
     * If id is null, creates a new task with generated ID.
     * Returns the ID of the saved task.
     */
    suspend fun saveTask(
        id: String?,
        title: String,
        notes: String?,
        dueDate: Long? = null,
        dueTime: Long? = null,
        reminder: Boolean = false,
        reminderTime: Long? = null,
    ): String {
        val taskId = id ?: UUID.randomUUID().toString()
        val existingTask = if (id != null) taskDao.getTask(id) else null

        if (existingTask != null) {
            // Update existing task - use sync repository to queue the operation
            syncRepository.updateTitleAndNotes(taskId, title, notes)
            // Update due date/time and reminder separately if they changed
            if (dueDate != existingTask.dueDate || dueTime != existingTask.dueTime ||
                reminder != existingTask.reminder || reminderTime != existingTask.reminderTime) {
                syncRepository.updateDueDate(taskId, dueDate, dueTime, reminder, reminderTime)
            }
        } else {
            // Create new task - use sync repository to queue the operation
            val timestamp = System.currentTimeMillis()
            val newTask = TaskEntity(
                id = taskId,
                title = title,
                notes = notes,
                updatedAt = timestamp,
                titleUpdatedAt = timestamp,
                notesUpdatedAt = timestamp,
                completedUpdatedAt = timestamp,
                deleted = false,
                completed = false,
                priority = 0,
                dirty = true,
                dueDate = dueDate,
                dueTime = dueTime,
                reminder = reminder,
                reminderTime = reminderTime,
            )
            syncRepository.createTask(newTask)
        }

        // Schedule or cancel reminder notification
        val savedTask = taskDao.getTask(taskId)
        if (savedTask != null) {
            notificationManager.scheduleReminder(savedTask)
        }

        return taskId
    }

    /**
     * Save a task with Long ID (for backward compatibility).
     */
    suspend fun saveTask(id: Long?, title: String, notes: String): String {
        return saveTask(id?.toString(), title, notes)
    }

    /**
     * Toggle task completion status with sync support.
     */
    suspend fun toggleComplete(id: String, completed: Boolean) {
        syncRepository.setCompleted(id, completed)
        if (completed) {
            notificationManager.cancelReminder(id)
        } else {
            // Re-schedule reminder if task is un-completed and has a reminder
            val task = taskDao.getTask(id)
            if (task != null) {
                notificationManager.scheduleReminder(task)
            }
        }
    }

    /**
     * Toggle task completion status with Long ID.
     */
    suspend fun toggleComplete(id: Long, completed: Boolean) {
        toggleComplete(id.toString(), completed)
    }

    /**
     * Soft delete a task with sync support.
     */
    suspend fun deleteTask(id: String) {
        notificationManager.cancelReminder(id)
        syncRepository.deleteTask(id)
    }

    /**
     * Soft delete a task with Long ID.
     */
    suspend fun deleteTask(id: Long) {
        deleteTask(id.toString())
    }

    /**
     * Previously used to insert sample data for testing/demo purposes.
     * Now empty - tasks come from sync with phone.
     */
    suspend fun insertSampleDataIfEmpty() {
        // No longer inserting sample data
        // Tasks are synced from the phone app
    }

    companion object {
        @Volatile
        private var INSTANCE: TaskRepository? = null

        /**
         * Get the singleton repository instance.
         */
        fun getInstance(context: Context): TaskRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TaskRepository(context).also { INSTANCE = it }
            }
        }
    }
}

/**
 * Extension function to convert TaskEntity to TaskLite.
 */
private fun TaskEntity.toTaskLite(): TaskLite {
    return TaskLite(
        id = this.id.toLongOrNull() ?: this.id.hashCode().toLong(),
        stringId = this.id, // Store original string ID for unique keys
        title = this.title,
        notes = this.notes ?: "",
        updatedAt = this.updatedAt,
        timestamp = this.timestamp,
        completed = this.completed,
        hidden = false,
        numSubtasks = 0,
        collapsed = false,
        indent = 0,
        repeating = this.repeating,
        priority = this.priority,
        dueDate = this.dueDate,
        dueTime = this.dueTime,
        reminder = this.reminder,
        reminderTime = this.reminderTime,
    )
}

/**
 * Extension function to convert TaskLite to TaskEntity.
 */
fun TaskLite.toEntity(): TaskEntity {
    return TaskEntity(
        id = this.stringId.ifEmpty { this.id.toString() },
        title = this.title,
        notes = this.notes.ifEmpty { null },
        updatedAt = this.updatedAt,
        deleted = false,
        completed = this.completed,
        priority = this.priority,
        timestamp = this.timestamp,
        repeating = this.repeating,
        dueDate = this.dueDate,
        dueTime = this.dueTime,
        reminder = this.reminder,
        reminderTime = this.reminderTime,
    )
}
