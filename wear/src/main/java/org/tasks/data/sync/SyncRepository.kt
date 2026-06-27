/**
 * SyncRepository.kt — Transactional layer for watch ↔ phone sync operations.
 *
 * ## Purpose
 * Every task mutation on the watch must be persisted **and** queued for sync
 * in the same Room transaction. This class wraps [TaskDao] + [OutboxOpDao]
 * together with `database.withTransaction { … }` to guarantee consistency.
 *
 * ## Watch → Phone (outbox)
 * - [createTask], [updateTitle], [updateNotes], [updateTitleAndNotes],
 *   [setCompleted], [deleteTask], [updateDueDate]
 * Each method:
 *   1. Writes the change to `tasks`.
 *   2. Inserts a matching [OutboxOpEntity] into `outbox_ops`.
 *
 * ## Phone → Watch (inbox)
 * - [processPhoneSnapshot] — merges a full task list from the phone.
 * - [processPhoneTaskUpdate] — applies a single task update with
 *   per-field last-writer-wins conflict resolution.
 * - [processPhoneDelete] — hard-deletes a task on watch.
 *
 * ## Idempotency
 * Uses [ProcessedOpDao] to record processed phone operation IDs
 * ([isPhoneOpProcessed] / [markPhoneOpProcessed]) so duplicate
 * deliveries are safely ignored.
 *
 * ## Cleanup helpers
 * [cleanupAckedOps], [cleanupOldProcessedOps], [cleanupDeletedTasks],
 * [resetStuckOps] — called periodically by [SyncWorker].
 *
 * ## Singleton
 * Accessed via [SyncRepository.getInstance].
 */
package org.tasks.data.sync

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import org.tasks.data.local.OutboxOpDao
import org.tasks.data.local.OutboxOpEntity
import org.tasks.data.local.OutboxOpState
import org.tasks.data.local.OutboxOpType
import org.tasks.data.local.ProcessedOpDao
import org.tasks.data.local.ProcessedOpEntity
import org.tasks.data.local.TaskDao
import org.tasks.data.local.TaskEntity
import org.tasks.data.local.WearDatabase
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID

/**
 * Repository that handles sync operations with transactional consistency.
 * Ensures that task modifications and outbox operations are always in sync.
 */
class SyncRepository(context: Context) {

    private val database = WearDatabase.getInstance(context)
    private val taskDao: TaskDao = database.taskDao()
    private val outboxOpDao: OutboxOpDao = database.outboxOpDao()
    private val processedOpDao: ProcessedOpDao = database.processedOpDao()

    // ===== Task operations with outbox (Watch -> Phone) =====

    /**
     * Create a new task and queue the operation for sync.
     * Uses a transaction to ensure atomicity.
     */
    suspend fun createTask(task: TaskEntity): String {
        val taskId = task.id.ifEmpty { UUID.randomUUID().toString() }
        val timestamp = System.currentTimeMillis()

        val newTask = task.copy(
            id = taskId,
            dirty = true,
            updatedAt = timestamp,
            titleUpdatedAt = timestamp,
            notesUpdatedAt = timestamp,
            completedUpdatedAt = timestamp,
        )

        val payload = createTaskPayload(newTask)
        val outboxOp = OutboxOpEntity(
            taskId = taskId,
            type = OutboxOpType.CREATE,
            payload = payload,
            createdAt = timestamp,
        )

        database.withTransaction {
            taskDao.insert(newTask)
            outboxOpDao.insert(outboxOp)
        }

        Timber.d("Created task $taskId and queued sync operation")
        return taskId
    }

    /**
     * Update task title and queue the operation for sync.
     */
    suspend fun updateTitle(taskId: String, title: String) {
        val timestamp = System.currentTimeMillis()

        val payload = JSONObject().apply {
            put(DataMapKeys.KEY_TASK_ID, taskId)
            put(DataMapKeys.KEY_TITLE, title)
            put(DataMapKeys.KEY_TITLE_UPDATED_AT, timestamp)
        }.toString()

        val outboxOp = OutboxOpEntity(
            taskId = taskId,
            type = OutboxOpType.UPDATE,
            payload = payload,
            createdAt = timestamp,
        )

        database.withTransaction {
            taskDao.updateTitle(taskId, title, timestamp)
            outboxOpDao.insert(outboxOp)
        }

        Timber.d("Updated title for task $taskId and queued sync operation")
    }

    /**
     * Update task notes and queue the operation for sync.
     */
    suspend fun updateNotes(taskId: String, notes: String?) {
        val timestamp = System.currentTimeMillis()

        val payload = JSONObject().apply {
            put(DataMapKeys.KEY_TASK_ID, taskId)
            put(DataMapKeys.KEY_NOTES, notes ?: "")
            put(DataMapKeys.KEY_NOTES_UPDATED_AT, timestamp)
        }.toString()

        val outboxOp = OutboxOpEntity(
            taskId = taskId,
            type = OutboxOpType.UPDATE,
            payload = payload,
            createdAt = timestamp,
        )

        database.withTransaction {
            taskDao.updateNotes(taskId, notes, timestamp)
            outboxOpDao.insert(outboxOp)
        }

        Timber.d("Updated notes for task $taskId and queued sync operation")
    }

    /**
     * Update task title and notes and queue the operation for sync.
     */
    suspend fun updateTitleAndNotes(taskId: String, title: String, notes: String?) {
        val timestamp = System.currentTimeMillis()

        val payload = JSONObject().apply {
            put(DataMapKeys.KEY_TASK_ID, taskId)
            put(DataMapKeys.KEY_TITLE, title)
            put(DataMapKeys.KEY_TITLE_UPDATED_AT, timestamp)
            put(DataMapKeys.KEY_NOTES, notes ?: "")
            put(DataMapKeys.KEY_NOTES_UPDATED_AT, timestamp)
        }.toString()

        val outboxOp = OutboxOpEntity(
            taskId = taskId,
            type = OutboxOpType.UPDATE,
            payload = payload,
            createdAt = timestamp,
        )

        database.withTransaction {
            taskDao.updateTitleAndNotes(taskId, title, notes, timestamp)
            outboxOpDao.insert(outboxOp)
        }

        Timber.d("Updated title and notes for task $taskId and queued sync operation")
    }

    /**
     * Update task due date and queue the operation for sync.
     */
    suspend fun updateDueDate(taskId: String, dueDate: Long?, dueTime: Long?, reminder: Boolean, reminderTime: Long?) {
        val timestamp = System.currentTimeMillis()

        val payload = JSONObject().apply {
            put(DataMapKeys.KEY_TASK_ID, taskId)
            put(DataMapKeys.KEY_DUE_DATE, dueDate ?: 0L)
            put(DataMapKeys.KEY_TIMESTAMP, timestamp)
        }.toString()

        val outboxOp = OutboxOpEntity(
            taskId = taskId,
            type = OutboxOpType.UPDATE,
            payload = payload,
            createdAt = timestamp,
        )

        database.withTransaction {
            taskDao.updateDueDateAndReminder(taskId, dueDate, dueTime, reminder, reminderTime, timestamp)
            outboxOpDao.insert(outboxOp)
        }

        Timber.d("Updated due date for task $taskId and queued sync operation")
    }

    /**
     * Toggle task completion and queue the operation for sync.
     */
    suspend fun setCompleted(taskId: String, completed: Boolean) {
        val timestamp = System.currentTimeMillis()

        val payload = JSONObject().apply {
            put(DataMapKeys.KEY_TASK_ID, taskId)
            put(DataMapKeys.KEY_COMPLETED, completed)
            put(DataMapKeys.KEY_COMPLETED_UPDATED_AT, timestamp)
        }.toString()

        val outboxOp = OutboxOpEntity(
            taskId = taskId,
            type = OutboxOpType.COMPLETE,
            payload = payload,
            createdAt = timestamp,
        )

        database.withTransaction {
            taskDao.setCompleted(taskId, completed, timestamp)
            outboxOpDao.insert(outboxOp)
        }

        Timber.d("Set completed=$completed for task $taskId and queued sync operation")
    }

    /**
     * Soft delete a task and queue the operation for sync.
     */
    suspend fun deleteTask(taskId: String) {
        val timestamp = System.currentTimeMillis()

        val payload = JSONObject().apply {
            put(DataMapKeys.KEY_TASK_ID, taskId)
            put(DataMapKeys.KEY_DELETED, true)
            put(DataMapKeys.KEY_TIMESTAMP, timestamp)
        }.toString()

        val outboxOp = OutboxOpEntity(
            taskId = taskId,
            type = OutboxOpType.DELETE,
            payload = payload,
            createdAt = timestamp,
        )

        database.withTransaction {
            taskDao.softDelete(taskId, timestamp)
            outboxOpDao.insert(outboxOp)
        }

        Timber.d("Deleted task $taskId and queued sync operation")
    }

    // ===== Outbox operations =====

    /**
     * Get pending operations to sync.
     */
    fun getPendingOps(): Flow<List<OutboxOpEntity>> = outboxOpDao.getPendingOps()

    /**
     * Get pending operations once.
     */
    suspend fun getPendingOpsOnce(): List<OutboxOpEntity> = outboxOpDao.getPendingOpsOnce()

    /**
     * Mark operation as being sent.
     */
    suspend fun markSending(opId: Long) = outboxOpDao.markSending(opId)

    /**
     * Mark operation as sent (waiting for ack).
     */
    suspend fun markSent(opId: Long) = outboxOpDao.markSent(opId)

    /**
     * Mark operation as acknowledged and clean up.
     */
    suspend fun markAcked(opId: Long) {
        database.withTransaction {
            outboxOpDao.markAcked(opId)
            // Optionally delete immediately or leave for batch cleanup
        }
        Timber.d("Operation $opId acknowledged")
    }

    /**
     * Mark operation as failed.
     */
    suspend fun markFailed(opId: Long, error: String?) = outboxOpDao.markFailed(opId, error)

    /**
     * Clean up acknowledged operations.
     */
    suspend fun cleanupAckedOps() = outboxOpDao.deleteAckedOps()

    /**
     * Reset stuck operations (e.g., after timeout).
     */
    suspend fun resetStuckOps(timeout: Long = 5 * 60 * 1000L) {
        val threshold = System.currentTimeMillis() - timeout
        outboxOpDao.resetStuckOps(threshold)
    }

    // ===== Incoming operations (Phone -> Watch) with conflict resolution =====

    /**
     * Check if an operation has already been processed (idempotency).
     */
    suspend fun isOpProcessed(opId: String): Boolean = processedOpDao.isProcessed(opId)

    /**
     * Apply an incoming task from phone with per-field conflict resolution.
     * Uses last-write-wins strategy per field.
     * Handles phone task ID mapping to avoid duplicates.
     */
    suspend fun applyIncomingTask(
        opId: String,
        taskId: String,
        title: String?,
        titleUpdatedAt: Long?,
        notes: String?,
        notesUpdatedAt: Long?,
        completed: Boolean?,
        completedUpdatedAt: Long?,
        deleted: Boolean?,
        priority: Int?,
        dueDate: Long? = null,
    ): Boolean {
        // Check idempotency
        if (processedOpDao.isProcessed(opId)) {
            Timber.d("Operation $opId already processed, skipping")
            return true
        }

        val phoneId = taskId.toLongOrNull()

        database.withTransaction {
            // Try to find task by phoneTaskId first, then by id
            var existingTask = if (phoneId != null) {
                taskDao.getTaskByPhoneId(phoneId) ?: taskDao.getTask(taskId)
            } else {
                taskDao.getTask(taskId)
            }

            // Handle deletion from phone
            if (deleted == true) {
                if (existingTask != null) {
                    // Hard delete the task since phone deleted it
                    taskDao.hardDelete(existingTask.id)
                    Timber.d("Hard deleted task ${existingTask.id} (phoneId: $phoneId) from phone")
                } else if (phoneId != null) {
                    // Also try to delete by phoneId directly
                    taskDao.hardDeleteByPhoneId(phoneId)
                    Timber.d("Hard deleted task by phoneId $phoneId from phone")
                }
                // Mark operation as processed
                processedOpDao.markProcessed(ProcessedOpEntity(opId))
                return@withTransaction
            }

            if (existingTask == null) {
                // Task doesn't exist locally, create it
                val newTask = TaskEntity(
                    id = taskId,
                    title = title ?: "",
                    titleUpdatedAt = titleUpdatedAt ?: System.currentTimeMillis(),
                    notes = notes,
                    notesUpdatedAt = notesUpdatedAt ?: System.currentTimeMillis(),
                    completed = completed ?: false,
                    completedUpdatedAt = completedUpdatedAt ?: System.currentTimeMillis(),
                    deleted = false,  // Never create as deleted
                    priority = priority ?: 0,
                    dirty = false,  // Not dirty since it came from phone
                    syncedAt = System.currentTimeMillis(),
                    phoneTaskId = phoneId,  // Store phone task ID for mapping
                    dueDate = dueDate,
                    reminder = dueDate != null && dueDate > 0,
                )
                taskDao.insert(newTask)
                Timber.d("Created new task $taskId from phone (phoneTaskId: $phoneId)")
            } else {
                // Task exists, apply per-field conflict resolution
                var updated = false

                // Update title if incoming is newer
                if (title != null && titleUpdatedAt != null && titleUpdatedAt > existingTask.titleUpdatedAt) {
                    taskDao.updateTitleIfNewer(taskId, title, titleUpdatedAt)
                    updated = true
                    Timber.d("Updated title for task $taskId (incoming newer)")
                } else if (title != null && titleUpdatedAt != null) {
                    Timber.d("Kept local title for task $taskId (local newer)")
                }

                // Update notes if incoming is newer
                if (notes != null && notesUpdatedAt != null && notesUpdatedAt > existingTask.notesUpdatedAt) {
                    taskDao.updateNotesIfNewer(taskId, notes, notesUpdatedAt)
                    updated = true
                    Timber.d("Updated notes for task $taskId (incoming newer)")
                } else if (notesUpdatedAt != null) {
                    Timber.d("Kept local notes for task $taskId (local newer)")
                }

                // Update completed if incoming is newer
                if (completed != null && completedUpdatedAt != null && completedUpdatedAt > existingTask.completedUpdatedAt) {
                    taskDao.updateCompletedIfNewer(taskId, completed, completedUpdatedAt)
                    updated = true
                    Timber.d("Updated completed for task $taskId (incoming newer)")
                } else if (completedUpdatedAt != null) {
                    Timber.d("Kept local completed for task $taskId (local newer)")
                }

                // Handle deletion
                if (deleted == true && !existingTask.deleted) {
                    taskDao.softDelete(taskId)
                    updated = true
                    Timber.d("Deleted task $taskId from phone")
                }

                if (updated) {
                    // Mark as synced since we just received from phone
                    taskDao.markSynced(taskId)
                }
            }

            // Mark operation as processed for idempotency
            processedOpDao.markProcessed(ProcessedOpEntity(opId))
        }

        return true
    }

    /**
     * Apply an incoming task from phone with phoneId for better duplicate detection.
     * This version is used for snapshots where we have the phone's task ID.
     */
    suspend fun applyIncomingTaskWithPhoneId(
        opId: String,
        taskId: String,
        phoneId: Long?,
        title: String?,
        titleUpdatedAt: Long?,
        notes: String?,
        notesUpdatedAt: Long?,
        completed: Boolean?,
        completedUpdatedAt: Long?,
        deleted: Boolean?,
        priority: Int?,
        dueDate: Long? = null,
    ): Boolean {
        database.withTransaction {
            // Try to find existing task by phoneTaskId first, then by taskId, then by phoneId as string
            var existingTask = if (phoneId != null && phoneId > 0) {
                taskDao.getTaskByPhoneId(phoneId)
                    ?: taskDao.getTask(taskId)
                    ?: taskDao.getTask(phoneId.toString())
            } else {
                taskDao.getTask(taskId)
            }

            // If not found, try to find a dirty local task with matching title
            // This helps match tasks created on watch with their phone counterparts
            if (existingTask == null && title != null) {
                existingTask = taskDao.findDirtyTaskByTitle(title)
                if (existingTask != null) {
                    Timber.d("Found matching dirty task by title: ${existingTask.id} for phone task $taskId")
                }
            }

            // Handle deletion from phone
            if (deleted == true) {
                if (existingTask != null) {
                    taskDao.hardDelete(existingTask.id)
                    Timber.d("Hard deleted task ${existingTask.id} (phoneId: $phoneId) from snapshot")
                } else if (phoneId != null && phoneId > 0) {
                    taskDao.hardDeleteByPhoneId(phoneId)
                    Timber.d("Hard deleted task by phoneId $phoneId from snapshot")
                }
                return@withTransaction
            }

            if (existingTask == null) {
                // Task doesn't exist locally, create it
                val newTask = TaskEntity(
                    id = taskId,
                    title = title ?: "",
                    titleUpdatedAt = titleUpdatedAt ?: System.currentTimeMillis(),
                    notes = notes,
                    notesUpdatedAt = notesUpdatedAt ?: System.currentTimeMillis(),
                    completed = completed ?: false,
                    completedUpdatedAt = completedUpdatedAt ?: System.currentTimeMillis(),
                    deleted = false,
                    priority = priority ?: 0,
                    dirty = false,
                    syncedAt = System.currentTimeMillis(),
                    phoneTaskId = phoneId,
                    dueDate = dueDate,
                    reminder = dueDate != null && dueDate > 0,
                )
                taskDao.insert(newTask)
                Timber.d("Created new task $taskId from snapshot (phoneTaskId: $phoneId, dueDate: $dueDate)")
            } else {
                // Task exists - update phoneTaskId if not set and apply per-field updates
                if (existingTask.phoneTaskId == null && phoneId != null && phoneId > 0) {
                    taskDao.setPhoneTaskId(existingTask.id, phoneId)
                    Timber.d("Linked local task ${existingTask.id} to phone task $phoneId")
                }

                // Update title if incoming is newer
                if (title != null && titleUpdatedAt != null && titleUpdatedAt > existingTask.titleUpdatedAt) {
                    taskDao.updateTitleIfNewer(existingTask.id, title, titleUpdatedAt)
                }

                // Update notes if incoming is newer
                if (notes != null && notesUpdatedAt != null && notesUpdatedAt > existingTask.notesUpdatedAt) {
                    taskDao.updateNotesIfNewer(existingTask.id, notes, notesUpdatedAt)
                }

                // Update completed if incoming is newer
                if (completed != null && completedUpdatedAt != null && completedUpdatedAt > existingTask.completedUpdatedAt) {
                    taskDao.updateCompletedIfNewer(existingTask.id, completed, completedUpdatedAt)
                }

                // Update dueDate from phone (phone is authoritative for due dates)
                if (dueDate != null && dueDate != (existingTask.dueDate ?: 0L)) {
                    taskDao.updateDueDateAndReminder(
                        id = existingTask.id,
                        dueDate = dueDate.takeIf { it > 0 },
                        dueTime = null,
                        reminder = dueDate > 0,
                        reminderTime = null,
                    )
                    Timber.d("Updated dueDate for task ${existingTask.id} to $dueDate")
                }

                taskDao.markSynced(existingTask.id)
            }
        }

        return true
    }

    /**
     * Apply a full snapshot from phone.
     * For each task in snapshot, apply with conflict resolution.
     */
    suspend fun applySnapshot(tasks: List<TaskSnapshotData>) {
        for (task in tasks) {
            // Use deterministic opId so re-applying the same snapshot doesn't create duplicates
            // We skip idempotency check for snapshots and always upsert instead
            applyIncomingTaskWithPhoneId(
                opId = "snapshot_${task.taskId}_${task.phoneId ?: 0}",
                taskId = task.taskId,
                phoneId = task.phoneId,
                title = task.title,
                titleUpdatedAt = task.titleUpdatedAt,
                notes = task.notes,
                notesUpdatedAt = task.notesUpdatedAt,
                completed = task.completed,
                completedUpdatedAt = task.completedUpdatedAt,
                deleted = task.deleted,
                priority = task.priority,
                dueDate = task.dueDate,
            )
        }
        Timber.d("Applied snapshot with ${tasks.size} tasks")
    }

    // ===== Cleanup =====

    /**
     * Clean up old processed operations (older than 7 days).
     */
    suspend fun cleanupOldProcessedOps(maxAge: Long = 7 * 24 * 60 * 60 * 1000L) {
        val threshold = System.currentTimeMillis() - maxAge
        processedOpDao.cleanupOld(threshold)
    }

    /**
     * Clean up deleted and synced tasks (older than 30 days).
     */
    suspend fun cleanupDeletedTasks(maxAge: Long = 30 * 24 * 60 * 60 * 1000L) {
        val threshold = System.currentTimeMillis() - maxAge
        taskDao.cleanupDeletedTasks(threshold)
    }

    // ===== Helpers =====

    private fun createTaskPayload(task: TaskEntity): String {
        return JSONObject().apply {
            put(DataMapKeys.KEY_TASK_ID, task.id)
            put(DataMapKeys.KEY_TITLE, task.title)
            put(DataMapKeys.KEY_TITLE_UPDATED_AT, task.titleUpdatedAt)
            put(DataMapKeys.KEY_NOTES, task.notes ?: "")
            put(DataMapKeys.KEY_NOTES_UPDATED_AT, task.notesUpdatedAt)
            put(DataMapKeys.KEY_COMPLETED, task.completed)
            put(DataMapKeys.KEY_COMPLETED_UPDATED_AT, task.completedUpdatedAt)
            put(DataMapKeys.KEY_DELETED, task.deleted)
            put(DataMapKeys.KEY_PRIORITY, task.priority)
            put(DataMapKeys.KEY_DUE_DATE, task.dueDate ?: 0L)
            put(DataMapKeys.KEY_TIMESTAMP, task.updatedAt)
        }.toString()
    }

    companion object {
        @Volatile
        private var INSTANCE: SyncRepository? = null

        fun getInstance(context: Context): SyncRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncRepository(context).also { INSTANCE = it }
            }
        }
    }
}

/**
 * Data class for task snapshot from phone.
 */
data class TaskSnapshotData(
    val taskId: String,
    val title: String?,
    val titleUpdatedAt: Long?,
    val notes: String?,
    val notesUpdatedAt: Long?,
    val completed: Boolean?,
    val completedUpdatedAt: Long?,
    val deleted: Boolean?,
    val priority: Int?,
    val phoneId: Long? = null,  // Phone task ID for mapping to avoid duplicates
    val dueDate: Long? = null,  // Due date timestamp from phone
)

