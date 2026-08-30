package org.tasks.wear

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.tasks.data.dao.TaskDao
import org.tasks.service.TaskCompleter
import com.todoroo.astrid.service.TaskCreator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.tasks.analytics.Firebase
import org.tasks.data.entity.Task
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync protocol constants shared between phone and watch.
 */
object SyncPaths {
    // Watch -> Phone operations
    const val WATCH_OUTBOX_PREFIX = "/outbox/watch/"
    fun watchAckPath(opId: Long) = "/ack/watch/$opId"

    // Phone -> Watch operations
    const val PHONE_OUTBOX_PREFIX = "/outbox/phone/"
    fun phoneAckPath(opId: String) = "/ack/phone/$opId"

    // Task updates from phone to watch
    const val TASK_UPDATE_PREFIX = "/tasks/"
    fun taskUpdatePath(taskId: String) = "$TASK_UPDATE_PREFIX$taskId"

    // Snapshot
    const val TASKS_SNAPSHOT = "/snapshot/tasks"

    // Sync request
    const val SYNC_REQUEST = "/sync/request"
}

object DataMapKeys {
    const val KEY_OP_ID = "opId"
    const val KEY_TASK_ID = "taskId"
    const val KEY_OP_TYPE = "opType"
    const val KEY_TIMESTAMP = "timestamp"

    const val KEY_TITLE = "title"
    const val KEY_TITLE_UPDATED_AT = "titleUpdatedAt"
    const val KEY_NOTES = "notes"
    const val KEY_NOTES_UPDATED_AT = "notesUpdatedAt"
    const val KEY_COMPLETED = "completed"
    const val KEY_COMPLETED_UPDATED_AT = "completedUpdatedAt"
    const val KEY_DELETED = "deleted"
    const val KEY_PRIORITY = "priority"
    const val KEY_REPEATING = "repeating"
    const val KEY_DUE_DATE = "dueDate"

    const val KEY_SUCCESS = "success"
    const val KEY_ERROR = "error"

    const val KEY_TASK_COUNT = "taskCount"
    const val KEY_SNAPSHOT_TIMESTAMP = "snapshotTimestamp"
}

/**
 * Sync manager for handling watch operations on the phone side.
 * Receives task operations from the watch and applies them to the phone database.
 */
@Singleton
@Suppress("TooManyFunctions", "LongParameterList", "LongMethod", "TooGenericExceptionCaught")
class PhoneSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val taskCreator: TaskCreator,
    private val taskCompleter: TaskCompleter,
    private val firebase: Firebase,
    private val alarmDao: org.tasks.data.dao.AlarmDao,
    private val workManager: org.tasks.jobs.WorkManager,
) : DataClient.OnDataChangedListener {

    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Start listening for data changes from watch.
     */
    fun startListening() {
        dataClient.addListener(this)
        Timber.d("PhoneSyncManager: Started listening for data changes")
    }

    /**
     * Stop listening for data changes.
     */
    fun stopListening() {
        dataClient.removeListener(this)
        Timber.d("PhoneSyncManager: Stopped listening for data changes")
    }

    override fun onDataChanged(events: DataEventBuffer) {
        Timber.d("PhoneSyncManager: Received ${events.count} data events")

        // IMPORTANT: DataEventBuffer is closed when onDataChanged returns,
        // so we must extract data BEFORE launching the coroutine
        val frozenEvents = mutableListOf<Pair<Int, DataEvent>>()
        for (event in events) {
            frozenEvents.add(Pair(event.type, event.freeze()))
        }

        scope.launch {
            for ((type, event) in frozenEvents) {
                when (type) {
                    DataEvent.TYPE_CHANGED -> handleDataChanged(event)
                    DataEvent.TYPE_DELETED -> { /* Usually no action needed */ }
                }
            }
        }
    }

    private suspend fun handleDataChanged(event: DataEvent) {
        val path = event.dataItem.uri.path ?: return
        Timber.d("PhoneSyncManager: Data changed: $path")

        when {
            // Operation from watch (create, update, delete, complete)
            path.startsWith(SyncPaths.WATCH_OUTBOX_PREFIX) -> {
                handleWatchOperation(event)
            }
            // Watch is requesting a full sync
            path == SyncPaths.SYNC_REQUEST -> {
                handleSyncRequest(event)
            }
            // Ack from watch for our operation (phone->watch) - just log it
            path.startsWith("/ack/phone/") -> {
                Timber.d("PhoneSyncManager: Received ack from watch: $path")
            }
        }
    }

    private suspend fun handleWatchOperation(event: DataEvent) {
        val path = event.dataItem.uri.path ?: return
        val opIdStr = path.removePrefix(SyncPaths.WATCH_OUTBOX_PREFIX)
        val opId = opIdStr.toLongOrNull() ?: return

        val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
        val dataMap = dataMapItem.dataMap

        val taskId = dataMap.getString(DataMapKeys.KEY_TASK_ID) ?: return
        val opType = dataMap.getString(DataMapKeys.KEY_OP_TYPE) ?: return

        Timber.d("PhoneSyncManager: Processing operation $opId of type $opType for task $taskId")

        try {
            when (opType) {
                "CREATE" -> handleCreate(taskId, dataMap)
                "UPDATE" -> handleUpdate(taskId, dataMap)
                "DELETE" -> handleDelete(taskId, dataMap)
                "COMPLETE" -> handleComplete(taskId, dataMap)
            }

            // Send ack to watch
            sendWatchAck(opId, true)

            // Delete the processed DataItem
            try {
                dataClient.deleteDataItems(event.dataItem.uri).await()
            } catch (e: Exception) {
                Timber.w(e, "Failed to delete DataItem after processing")
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to process operation $opId")
            sendWatchAck(opId, false, e.message)
        }
    }

    private suspend fun handleCreate(taskId: String, dataMap: com.google.android.gms.wearable.DataMap) {
        val title = dataMap.getString(DataMapKeys.KEY_TITLE) ?: "Untitled"
        val notes = dataMap.getString(DataMapKeys.KEY_NOTES)
        val completed = if (dataMap.containsKey(DataMapKeys.KEY_COMPLETED))
            dataMap.getBoolean(DataMapKeys.KEY_COMPLETED) else false
        val priority = if (dataMap.containsKey(DataMapKeys.KEY_PRIORITY))
            dataMap.getInt(DataMapKeys.KEY_PRIORITY) else Task.Priority.NONE
        val dueDate = dataMap.getLong(DataMapKeys.KEY_DUE_DATE).takeIf { it > 0 }

        // Check if a task with this remoteId (watch task ID) already exists to prevent duplicates
        val existingTask = taskDao.fetch(taskId)
        if (existingTask != null) {
            Timber.d("PhoneSyncManager: Task with remoteId $taskId already exists (id=${existingTask.id}), updating instead")
            val updated = existingTask.copy(
                title = title,
                notes = notes ?: existingTask.notes,
                completionDate = if (completed) System.currentTimeMillis() else existingTask.completionDate,
                priority = priority,
                dueDate = dueDate ?: existingTask.dueDate,
                modificationDate = System.currentTimeMillis(),
            )
            taskDao.update(updated)
            // If due date changed, update alarms
            if (dueDate != null && dueDate != existingTask.dueDate && dueDate > 0) {
                alarmDao.insert(org.tasks.data.entity.Alarm.whenDue(existingTask.id))
                workManager.triggerNotifications()
            }
            return
        }

        // Create new task using TaskCreator
        val task = taskCreator.basicQuickAddTask(title, remoteId = taskId)

        // Update task with remoteId, notes, and other fields
        taskDao.fetch(task.id)?.let { fetchedTask ->
            val updated = fetchedTask.copy(
                remoteId = taskId, // Store watch task ID so we can find it later
                notes = notes ?: fetchedTask.notes,
                completionDate = if (completed) System.currentTimeMillis() else fetchedTask.completionDate,
                priority = priority,
                dueDate = dueDate ?: fetchedTask.dueDate,
            )
            taskDao.update(updated)

            // If a due date is set, create default "when due" alarm for notifications
            if (dueDate != null && dueDate > 0) {
                alarmDao.insert(org.tasks.data.entity.Alarm.whenDue(fetchedTask.id))
                workManager.triggerNotifications()
            }
        }

        firebase.addTask("wearable")
        Timber.d("PhoneSyncManager: Created task ${task.id} from watch operation (remoteId: $taskId, dueDate: $dueDate)")
    }

    private suspend fun handleUpdate(taskId: String, dataMap: com.google.android.gms.wearable.DataMap) {
        // Try to find task by numeric ID first, then by remoteId (for watch-created tasks)
        val task = taskId.toLongOrNull()?.let { taskDao.fetch(it) }
            ?: taskDao.fetch(taskId) // Search by remoteId

        if (task == null) {
            Timber.w("PhoneSyncManager: Task $taskId not found for update, creating instead")
            handleCreate(taskId, dataMap)
            return
        }

        val phoneModTime = task.modificationDate

        // Extract incoming field values and their timestamps
        val title = dataMap.getString(DataMapKeys.KEY_TITLE)
        val titleUpdatedAt = if (dataMap.containsKey(DataMapKeys.KEY_TITLE_UPDATED_AT))
            dataMap.getLong(DataMapKeys.KEY_TITLE_UPDATED_AT) else null
        val notes = dataMap.getString(DataMapKeys.KEY_NOTES)
        val notesUpdatedAt = if (dataMap.containsKey(DataMapKeys.KEY_NOTES_UPDATED_AT))
            dataMap.getLong(DataMapKeys.KEY_NOTES_UPDATED_AT) else null
        val completed = if (dataMap.containsKey(DataMapKeys.KEY_COMPLETED))
            dataMap.getBoolean(DataMapKeys.KEY_COMPLETED) else null
        val completedUpdatedAt = if (dataMap.containsKey(DataMapKeys.KEY_COMPLETED_UPDATED_AT))
            dataMap.getLong(DataMapKeys.KEY_COMPLETED_UPDATED_AT) else null
        val dueDate = dataMap.getLong(DataMapKeys.KEY_DUE_DATE).takeIf { it > 0 }

        // Per-field last-write-wins: only apply fields where watch timestamp is newer
        var newTitle = task.title
        var newNotes = task.notes
        var newCompletionDate = task.completionDate
        var newDueDate = task.dueDate
        var modified = false

        // Title: apply if watch timestamp is newer than phone modification date
        if (title != null && titleUpdatedAt != null && titleUpdatedAt > phoneModTime) {
            newTitle = title
            modified = true
            Timber.d("PhoneSyncManager: Accepting newer title for task ${task.id}")
        } else if (title != null) {
            Timber.d("PhoneSyncManager: Keeping phone title for task ${task.id} (phone newer)")
        }

        // Notes: apply if watch timestamp is newer
        if (notes != null && notesUpdatedAt != null && notesUpdatedAt > phoneModTime) {
            newNotes = notes
            modified = true
            Timber.d("PhoneSyncManager: Accepting newer notes for task ${task.id}")
        } else if (notes != null) {
            Timber.d("PhoneSyncManager: Keeping phone notes for task ${task.id} (phone newer)")
        }

        // Completed: apply if watch timestamp is newer
        if (completed != null && completedUpdatedAt != null && completedUpdatedAt > phoneModTime) {
            newCompletionDate = when {
                completed && !task.isCompleted -> System.currentTimeMillis()
                !completed -> 0
                else -> task.completionDate
            }
            modified = true
            Timber.d("PhoneSyncManager: Accepting newer completed=$completed for task ${task.id}")
        } else if (completed != null) {
            Timber.d("PhoneSyncManager: Keeping phone completed for task ${task.id} (phone newer)")
        }

        // DueDate: apply if different (no per-field timestamp available for due date)
        val oldDueDate = task.dueDate
        if (dueDate != null && dueDate != oldDueDate) {
            newDueDate = dueDate
            modified = true
        }

        if (!modified) {
            Timber.d("PhoneSyncManager: No fields updated for task ${task.id} (all phone fields newer)")
            return
        }

        val updatedTask = task.copy(
            title = newTitle,
            notes = newNotes,
            dueDate = newDueDate,
            completionDate = newCompletionDate,
            modificationDate = System.currentTimeMillis(),
        )

        taskDao.update(updatedTask)

        // If due date was added or changed, create alarm and trigger notifications
        if (newDueDate != oldDueDate && newDueDate > 0) {
            val existingAlarms = alarmDao.getAlarms(task.id)
            val hasWhenDueAlarm = existingAlarms.any { it.type == org.tasks.data.entity.Alarm.TYPE_REL_END }
            if (!hasWhenDueAlarm) {
                alarmDao.insert(org.tasks.data.entity.Alarm.whenDue(task.id))
            }
            workManager.triggerNotifications()
        }

        Timber.d("PhoneSyncManager: Updated task ${task.id} from watch operation with conflict resolution")
    }

    private suspend fun handleDelete(taskId: String, @Suppress("UNUSED_PARAMETER") dataMap: com.google.android.gms.wearable.DataMap) {
        val task = taskId.toLongOrNull()?.let { taskDao.fetch(it) }
            ?: taskDao.fetch(taskId)

        if (task == null) {
            Timber.w("PhoneSyncManager: Task $taskId not found for delete")
            return
        }

        taskDao.update(task.copy(deletionDate = System.currentTimeMillis()))
        Timber.d("PhoneSyncManager: Deleted task ${task.id} from watch operation")
    }

    private suspend fun handleComplete(taskId: String, dataMap: com.google.android.gms.wearable.DataMap) {
        val task = taskId.toLongOrNull()?.let { taskDao.fetch(it) }
            ?: taskDao.fetch(taskId)

        if (task == null) {
            Timber.w("PhoneSyncManager: Task $taskId not found for complete")
            return
        }

        val completed = dataMap.getBoolean(DataMapKeys.KEY_COMPLETED)
        taskCompleter.setComplete(task.id, completed)
        firebase.completeTask("wearable")
        Timber.d("PhoneSyncManager: Set task ${task.id} completed=$completed from watch operation")
    }

    private suspend fun sendWatchAck(opId: Long, success: Boolean, error: String? = null) {
        val path = SyncPaths.watchAckPath(opId)
        val request = PutDataMapRequest.create(path).apply {
            dataMap.putLong(DataMapKeys.KEY_OP_ID, opId)
            dataMap.putBoolean(DataMapKeys.KEY_SUCCESS, success)
            if (error != null) {
                dataMap.putString(DataMapKeys.KEY_ERROR, error)
            }
            dataMap.putLong(DataMapKeys.KEY_TIMESTAMP, System.currentTimeMillis())
            setUrgent()
        }

        try {
            dataClient.putDataItem(request.asPutDataRequest()).await()
            Timber.d("PhoneSyncManager: Sent ack for operation $opId (success=$success)")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send ack for operation $opId")
        }
    }

    private suspend fun handleSyncRequest(@Suppress("UNUSED_PARAMETER") event: DataEvent) {
        Timber.d("PhoneSyncManager: Watch requested sync")

        // Get all non-deleted tasks
        val tasks = taskDao.getActiveTasks()

        sendTaskSnapshotInternal(tasks)
    }

    /**
     * Send a snapshot of all active tasks to watch.
     * Public method for use by WearRefresherImpl.
     */
    suspend fun sendTaskSnapshot() {
        val tasks = taskDao.getActiveTasks()
        sendTaskSnapshotInternal(tasks)
    }

    private suspend fun sendTaskSnapshotInternal(tasks: List<Task>) {
        val path = SyncPaths.TASKS_SNAPSHOT
        val request = PutDataMapRequest.create(path).apply {
            dataMap.putInt(DataMapKeys.KEY_TASK_COUNT, tasks.size)
            dataMap.putLong(DataMapKeys.KEY_SNAPSHOT_TIMESTAMP, System.currentTimeMillis())

            // Serialize each task
            tasks.forEachIndexed { index, task ->
                val prefix = "task_$index"
                // Use remoteId (watch UUID) if available, otherwise phone ID as string
                val taskIdForWatch = task.remoteId?.takeIf { it.isNotBlank() } ?: task.id.toString()
                dataMap.putString("${prefix}_id", taskIdForWatch)
                dataMap.putString("${prefix}_title", task.title ?: "")
                dataMap.putLong("${prefix}_titleUpdatedAt", task.modificationDate)
                dataMap.putString("${prefix}_notes", task.notes ?: "")
                dataMap.putLong("${prefix}_notesUpdatedAt", task.modificationDate)
                dataMap.putBoolean("${prefix}_completed", task.isCompleted)
                dataMap.putLong("${prefix}_completedUpdatedAt",
                    if (task.isCompleted) task.completionDate else task.modificationDate)
                dataMap.putBoolean("${prefix}_deleted", task.deletionDate > 0)
                // Send phoneId so watch can properly link and deduplicate
                dataMap.putLong("${prefix}_phoneId", task.id)
                dataMap.putInt("${prefix}_priority", task.priority)
                dataMap.putLong("${prefix}_dueDate", task.dueDate)
            }
        }

        try {
            dataClient.putDataItem(request.asPutDataRequest()).await()
            Timber.d("PhoneSyncManager: Sent snapshot with ${tasks.size} tasks")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send task snapshot")
        }
    }

    /**
     * Send a single task update to watch.
     */
    suspend fun sendTaskUpdate(task: Task) {
        // Use remoteId (watch UUID) if available, otherwise phone ID as string
        val taskIdForWatch = task.remoteId?.takeIf { it.isNotBlank() } ?: task.id.toString()
        val path = SyncPaths.taskUpdatePath(taskIdForWatch)
        val request = PutDataMapRequest.create(path).apply {
            dataMap.putString(DataMapKeys.KEY_TASK_ID, taskIdForWatch)
            dataMap.putString(DataMapKeys.KEY_TITLE, task.title ?: "")
            dataMap.putLong(DataMapKeys.KEY_TITLE_UPDATED_AT, task.modificationDate)
            dataMap.putString(DataMapKeys.KEY_NOTES, task.notes ?: "")
            dataMap.putLong(DataMapKeys.KEY_NOTES_UPDATED_AT, task.modificationDate)
            dataMap.putBoolean(DataMapKeys.KEY_COMPLETED, task.isCompleted)
            dataMap.putLong(DataMapKeys.KEY_COMPLETED_UPDATED_AT,
                if (task.isCompleted) task.completionDate else task.modificationDate)
            dataMap.putBoolean(DataMapKeys.KEY_DELETED, task.deletionDate > 0)
            dataMap.putInt(DataMapKeys.KEY_PRIORITY, task.priority)
            dataMap.putBoolean(DataMapKeys.KEY_REPEATING, !task.recurrence.isNullOrBlank())
            dataMap.putLong(DataMapKeys.KEY_DUE_DATE, task.dueDate)
            dataMap.putLong(DataMapKeys.KEY_TIMESTAMP, System.currentTimeMillis())
            // Send phoneId so watch can properly link and deduplicate
            dataMap.putLong("phoneId", task.id)
            setUrgent()
        }

        try {
            dataClient.putDataItem(request.asPutDataRequest()).await()
            Timber.d("PhoneSyncManager: Sent task update for ${task.id} (watchId: $taskIdForWatch)")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send task update for ${task.id}")
        }
    }

    /**
     * Notify watch about task changes on phone.
     */
    suspend fun notifyTaskChanged(taskId: Long) {
        val task = taskDao.fetch(taskId) ?: return
        sendTaskUpdate(task)
    }

    /**
     * Notify watch about task deletion on phone.
     */
    suspend fun notifyTaskDeleted(taskId: Long) {
        val task = taskDao.fetch(taskId)
        val taskIdForWatch = task?.remoteId?.takeIf { it.isNotBlank() } ?: taskId.toString()
        val path = SyncPaths.taskUpdatePath(taskIdForWatch)
        val request = PutDataMapRequest.create(path).apply {
            dataMap.putString(DataMapKeys.KEY_TASK_ID, taskIdForWatch)
            dataMap.putBoolean(DataMapKeys.KEY_DELETED, true)
            dataMap.putLong(DataMapKeys.KEY_TIMESTAMP, System.currentTimeMillis())
            dataMap.putLong("phoneId", taskId)
            setUrgent()
        }

        try {
            dataClient.putDataItem(request.asPutDataRequest()).await()
            Timber.d("PhoneSyncManager: Sent delete notification for task $taskId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send delete notification for task $taskId")
        }
    }
}

