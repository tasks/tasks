/**
 * SyncProtocol.kt — Constants that define the Data Layer sync protocol.
 *
 * ## Path conventions
 * | Path pattern                | Direction      | Description                         |
 * |-----------------------------|----------------|-------------------------------------|
 * | `/outbox/watch/{opId}`      | Watch → Phone  | Queued operation from watch          |
 * | `/ack/watch/{opId}`         | Phone → Watch  | Ack for a watch operation            |
 * | `/outbox/phone/{opId}`      | Phone → Watch  | Queued operation from phone          |
 * | `/ack/phone/{opId}`         | Watch → Phone  | Ack for a phone operation            |
 * | `/snapshot/tasks`           | Phone → Watch  | Full task list snapshot              |
 * | `/tasks/{taskId}`           | Phone → Watch  | Single-task incremental update       |
 *
 * ## Key objects
 * - [SyncPaths] — path builders and extractors.
 * - [DataMapKeys] — string keys used inside `DataMap` items.
 * - [OpTypes] — operation type constants (`CREATE`, `UPDATE`, etc.).
 */
package org.tasks.data.sync

/**
 * Constants for Data Layer sync protocol between Watch and Phone.
 *
 * Path structure:
 * - /outbox/watch/{opId}  -> Operations from watch to phone
 * - /ack/watch/{opId}     -> Acknowledgments from phone to watch
 * - /outbox/phone/{opId}  -> Operations from phone to watch
 * - /ack/phone/{opId}     -> Acknowledgments from watch to phone
 * - /snapshot/tasks       -> Full task snapshot from phone to watch
 */
object SyncPaths {
    // Watch -> Phone operations
    const val WATCH_OUTBOX_PREFIX = "/outbox/watch/"
    fun watchOutboxPath(opId: Long) = "$WATCH_OUTBOX_PREFIX$opId"

    // Phone -> Watch acknowledgments
    const val WATCH_ACK_PREFIX = "/ack/watch/"
    fun watchAckPath(opId: Long) = "$WATCH_ACK_PREFIX$opId"

    // Phone -> Watch operations
    const val PHONE_OUTBOX_PREFIX = "/outbox/phone/"
    fun phoneOutboxPath(opId: String) = "$PHONE_OUTBOX_PREFIX$opId"

    // Watch -> Phone acknowledgments
    const val PHONE_ACK_PREFIX = "/ack/phone/"
    fun phoneAckPath(opId: String) = "$PHONE_ACK_PREFIX$opId"

    // Full snapshot from phone to watch
    const val TASKS_SNAPSHOT = "/snapshot/tasks"

    // Single task update from phone
    const val TASK_UPDATE_PREFIX = "/tasks/"
    fun taskUpdatePath(taskId: String) = "$TASK_UPDATE_PREFIX$taskId"

    // Extract opId from path
    fun extractWatchOpId(path: String): Long? {
        return when {
            path.startsWith(WATCH_OUTBOX_PREFIX) -> path.removePrefix(WATCH_OUTBOX_PREFIX).toLongOrNull()
            path.startsWith(WATCH_ACK_PREFIX) -> path.removePrefix(WATCH_ACK_PREFIX).toLongOrNull()
            else -> null
        }
    }

    fun extractPhoneOpId(path: String): String? {
        return when {
            path.startsWith(PHONE_OUTBOX_PREFIX) -> path.removePrefix(PHONE_OUTBOX_PREFIX).takeIf { it.isNotEmpty() }
            path.startsWith(PHONE_ACK_PREFIX) -> path.removePrefix(PHONE_ACK_PREFIX).takeIf { it.isNotEmpty() }
            else -> null
        }
    }

    fun extractTaskId(path: String): String? {
        return if (path.startsWith(TASK_UPDATE_PREFIX)) {
            path.removePrefix(TASK_UPDATE_PREFIX).takeIf { it.isNotEmpty() }
        } else null
    }
}

/**
 * Keys for DataMap serialization.
 */
object DataMapKeys {
    // Common keys
    const val KEY_OP_ID = "opId"
    const val KEY_TASK_ID = "taskId"
    const val KEY_OP_TYPE = "opType"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_URGENT = "urgent"

    // Task field keys
    const val KEY_TITLE = "title"
    const val KEY_TITLE_UPDATED_AT = "titleUpdatedAt"
    const val KEY_NOTES = "notes"
    const val KEY_NOTES_UPDATED_AT = "notesUpdatedAt"
    const val KEY_COMPLETED = "completed"
    const val KEY_COMPLETED_UPDATED_AT = "completedUpdatedAt"
    const val KEY_DELETED = "deleted"
    const val KEY_PRIORITY = "priority"
    const val KEY_DUE_DATE = "dueDate"
    const val KEY_REPEATING = "repeating"

    // Snapshot keys
    const val KEY_TASKS = "tasks"
    const val KEY_SNAPSHOT_TIMESTAMP = "snapshotTimestamp"
    const val KEY_TASK_COUNT = "taskCount"

    // Ack keys
    const val KEY_SUCCESS = "success"
    const val KEY_ERROR = "error"
}

/**
 * Operation types for sync.
 */
object OpTypes {
    const val CREATE = "CREATE"
    const val UPDATE = "UPDATE"
    const val DELETE = "DELETE"
    const val COMPLETE = "COMPLETE"
    const val UPDATE_TITLE = "UPDATE_TITLE"
    const val UPDATE_NOTES = "UPDATE_NOTES"
}
