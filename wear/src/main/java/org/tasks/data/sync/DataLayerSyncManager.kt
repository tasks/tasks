/**
 * DataLayerSyncManager.kt — Bridges the Wearable Data Layer API with Room.
 *
 * ## Responsibilities
 * 1. **Outbound** ([processPendingOps]):
 *    Reads pending [OutboxOpEntity] rows, serialises them into
 *    [PutDataMapRequest] items at `/outbox/watch/{opId}` and pushes
 *    them via [DataClient].
 *
 * 2. **Inbound** ([onDataChanged]):
 *    Listens for [DataEvent]s.  Routes each event by path prefix:
 *    - `/ack/watch/{id}`    → marks the outbox op as acknowledged.
 *    - `/outbox/phone/{id}` → calls [SyncRepository.applyIncomingTask]
 *                              and sends an ack back.
 *    - `/snapshot/tasks`    → full snapshot merge.
 *    - `/tasks/{id}`        → single-task incremental update.
 *
 * 3. **State** ([syncState]):
 *    Exposes a [SyncState] flow (`IDLE`, `SYNCING`, `ERROR`)
 *    consumed by the settings screen's connection-status chip.
 *
 * ## Threading
 * All work runs on a dedicated [CoroutineScope] backed by
 * `Dispatchers.IO + SupervisorJob`.
 *
 * ## Singleton
 * Accessed via [DataLayerSyncManager.getInstance].
 */

package org.tasks.data.sync

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import org.tasks.data.local.OutboxOpEntity
import org.tasks.data.local.OutboxOpType
import timber.log.Timber

/**
 * Manager for syncing data between Watch and Phone via Wearable Data Layer API.
 *
 * Uses DataClient (DataItem) for persistent sync:
 * - DataItems are automatically synced when connectivity is available
 * - System handles retry and delivery
 * - Supports urgent (immediate) and batched delivery
 */
class DataLayerSyncManager(
    private val context: Context,
    private val syncRepository: SyncRepository,
) : DataClient.OnDataChangedListener {

    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState

    /**
     * Start listening for data changes from phone.
     */
    fun startListening() {
        dataClient.addListener(this)
        Timber.d("Started listening for data changes")
    }

    /**
     * Stop listening for data changes.
     */
    fun stopListening() {
        dataClient.removeListener(this)
        Timber.d("Stopped listening for data changes")
    }

    /**
     * Process pending outbox operations and send them to phone.
     */
    suspend fun processPendingOps() {
        val pendingOps = syncRepository.getPendingOpsOnce()
        if (pendingOps.isEmpty()) {
            Timber.d("No pending operations to sync")
            return
        }

        _syncState.value = SyncState.SYNCING
        Timber.d("Processing ${pendingOps.size} pending operations")

        for (op in pendingOps) {
            try {
                sendOperation(op)
            } catch (e: Exception) {
                Timber.e(e, "Failed to send operation ${op.opId}")
                syncRepository.markFailed(op.opId, e.message)
            }
        }

        _syncState.value = SyncState.IDLE
    }

    /**
     * Send a single operation to phone via DataItem.
     */
    private suspend fun sendOperation(op: OutboxOpEntity) {
        syncRepository.markSending(op.opId)

        val path = SyncPaths.watchOutboxPath(op.opId)
        val request = PutDataMapRequest.create(path).apply {
            dataMap.putLong(DataMapKeys.KEY_OP_ID, op.opId)
            dataMap.putString(DataMapKeys.KEY_TASK_ID, op.taskId)
            dataMap.putString(DataMapKeys.KEY_OP_TYPE, op.type.name)
            dataMap.putString("payload", op.payload)
            dataMap.putLong(DataMapKeys.KEY_TIMESTAMP, op.createdAt)

            // Parse payload JSON and add fields to DataMap for easier processing on phone
            try {
                val payload = JSONObject(op.payload)
                when (op.type) {
                    OutboxOpType.CREATE, OutboxOpType.UPDATE -> {
                        payload.optString(DataMapKeys.KEY_TITLE)?.let {
                            dataMap.putString(DataMapKeys.KEY_TITLE, it)
                        }
                        payload.optLong(DataMapKeys.KEY_TITLE_UPDATED_AT).takeIf { it > 0 }?.let {
                            dataMap.putLong(DataMapKeys.KEY_TITLE_UPDATED_AT, it)
                        }
                        payload.optString(DataMapKeys.KEY_NOTES)?.let {
                            dataMap.putString(DataMapKeys.KEY_NOTES, it)
                        }
                        payload.optLong(DataMapKeys.KEY_NOTES_UPDATED_AT).takeIf { it > 0 }?.let {
                            dataMap.putLong(DataMapKeys.KEY_NOTES_UPDATED_AT, it)
                        }
                        if (payload.has(DataMapKeys.KEY_COMPLETED)) {
                            dataMap.putBoolean(DataMapKeys.KEY_COMPLETED, payload.getBoolean(DataMapKeys.KEY_COMPLETED))
                        }
                        payload.optLong(DataMapKeys.KEY_COMPLETED_UPDATED_AT).takeIf { it > 0 }?.let {
                            dataMap.putLong(DataMapKeys.KEY_COMPLETED_UPDATED_AT, it)
                        }
                        if (payload.has(DataMapKeys.KEY_PRIORITY)) {
                            dataMap.putInt(DataMapKeys.KEY_PRIORITY, payload.getInt(DataMapKeys.KEY_PRIORITY))
                        }
                        payload.optLong(DataMapKeys.KEY_DUE_DATE).takeIf { it > 0 }?.let {
                            dataMap.putLong(DataMapKeys.KEY_DUE_DATE, it)
                        }
                    }
                    OutboxOpType.COMPLETE -> {
                        dataMap.putBoolean(DataMapKeys.KEY_COMPLETED, payload.getBoolean(DataMapKeys.KEY_COMPLETED))
                        dataMap.putLong(DataMapKeys.KEY_COMPLETED_UPDATED_AT, payload.getLong(DataMapKeys.KEY_COMPLETED_UPDATED_AT))
                    }
                    OutboxOpType.DELETE -> {
                        dataMap.putBoolean(DataMapKeys.KEY_DELETED, true)
                        dataMap.putLong(DataMapKeys.KEY_TIMESTAMP, payload.getLong(DataMapKeys.KEY_TIMESTAMP))
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse payload for operation ${op.opId}")
            }
        }

        // Use urgent for user-initiated actions (create, update, delete)
        // Non-urgent for batch syncs
        val shouldBeUrgent = op.type in listOf(OutboxOpType.CREATE, OutboxOpType.UPDATE, OutboxOpType.DELETE, OutboxOpType.COMPLETE)
        if (shouldBeUrgent) {
            request.setUrgent()
        }

        val putDataRequest = request.asPutDataRequest()
        dataClient.putDataItem(putDataRequest).await()
        syncRepository.markSent(op.opId)

        Timber.d("Sent operation ${op.opId} (${op.type}) for task ${op.taskId}")
    }

    /**
     * Handle data changes from phone.
     * NOTE: DataEventBuffer is closed after onDataChanged returns,
     * so we must extract data BEFORE launching coroutines.
     */
    override fun onDataChanged(events: DataEventBuffer) {
        Timber.d("Received ${events.count} data events")

        // Extract all data synchronously BEFORE the buffer is closed
        val dataEvents = mutableListOf<Pair<Int, DataEvent>>()
        for (event in events) {
            // Freeze/copy the event data to avoid buffer-closed errors
            dataEvents.add(Pair(event.type, event.freeze()))
        }

        // Now we can safely process asynchronously
        scope.launch {
            for ((type, frozenEvent) in dataEvents) {
                try {
                    when (type) {
                        DataEvent.TYPE_CHANGED -> handleDataChanged(frozenEvent)
                        DataEvent.TYPE_DELETED -> handleDataDeleted(frozenEvent)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error processing data event")
                }
            }
        }
    }

    private suspend fun handleDataChanged(event: DataEvent) {
        val path = event.dataItem.uri.path ?: return
        Timber.d("Data changed: $path")

        when {
            // Acknowledgment from phone for our operation
            path.startsWith(SyncPaths.WATCH_ACK_PREFIX) -> {
                handleWatchAck(event)
            }
            // Task update from phone
            path.startsWith(SyncPaths.PHONE_OUTBOX_PREFIX) -> {
                handlePhoneOperation(event)
            }
            // Single task update from phone
            path.startsWith(SyncPaths.TASK_UPDATE_PREFIX) -> {
                handleTaskUpdate(event)
            }
            // Full snapshot from phone
            path == SyncPaths.TASKS_SNAPSHOT -> {
                handleSnapshot(event)
            }
        }
    }

    /**
     * Handle acknowledgment from phone for our operation.
     */
    private suspend fun handleWatchAck(event: DataEvent) {
        val path = event.dataItem.uri.path ?: return
        val opId = SyncPaths.extractWatchOpId(path) ?: return

        val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
        val dataMap = dataMapItem.dataMap
        val success = dataMap.getBoolean(DataMapKeys.KEY_SUCCESS, true)

        if (success) {
            syncRepository.markAcked(opId)
            Timber.d("Received ack for operation $opId")

            // Delete the DataItem now that it's acked
            try {
                dataClient.deleteDataItems(event.dataItem.uri).await()
            } catch (e: Exception) {
                Timber.w(e, "Failed to delete ack DataItem")
            }
        } else {
            val error = dataMap.getString(DataMapKeys.KEY_ERROR)
            syncRepository.markFailed(opId, error)
            Timber.w("Operation $opId failed on phone: $error")
        }
    }

    /**
     * Handle operation from phone (create, update, delete).
     */
    private suspend fun handlePhoneOperation(event: DataEvent) {
        val path = event.dataItem.uri.path ?: return
        val opId = SyncPaths.extractPhoneOpId(path) ?: return

        // Check idempotency
        if (syncRepository.isOpProcessed(opId)) {
            Timber.d("Operation $opId already processed, sending ack")
            sendPhoneAck(opId, true)
            return
        }

        val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
        val dataMap = dataMapItem.dataMap

        val taskId = dataMap.getString(DataMapKeys.KEY_TASK_ID) ?: return
        val opType = dataMap.getString(DataMapKeys.KEY_OP_TYPE)

        try {
            val result = syncRepository.applyIncomingTask(
                opId = opId,
                taskId = taskId,
                title = dataMap.getString(DataMapKeys.KEY_TITLE),
                titleUpdatedAt = dataMap.getLong(DataMapKeys.KEY_TITLE_UPDATED_AT).takeIf { it > 0 },
                notes = dataMap.getString(DataMapKeys.KEY_NOTES),
                notesUpdatedAt = dataMap.getLong(DataMapKeys.KEY_NOTES_UPDATED_AT).takeIf { it > 0 },
                completed = if (dataMap.containsKey(DataMapKeys.KEY_COMPLETED)) dataMap.getBoolean(DataMapKeys.KEY_COMPLETED) else null,
                completedUpdatedAt = dataMap.getLong(DataMapKeys.KEY_COMPLETED_UPDATED_AT).takeIf { it > 0 },
                deleted = if (dataMap.containsKey(DataMapKeys.KEY_DELETED)) dataMap.getBoolean(DataMapKeys.KEY_DELETED) else null,
                priority = if (dataMap.containsKey(DataMapKeys.KEY_PRIORITY)) dataMap.getInt(DataMapKeys.KEY_PRIORITY) else null,
                dueDate = dataMap.getLong(DataMapKeys.KEY_DUE_DATE).takeIf { it > 0 },
            )

            sendPhoneAck(opId, result)
            Timber.d("Applied phone operation $opId ($opType) for task $taskId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply phone operation $opId")
            sendPhoneAck(opId, false, e.message)
        }
    }

    /**
     * Handle single task update from phone.
     */
    private suspend fun handleTaskUpdate(event: DataEvent) {
        val path = event.dataItem.uri.path ?: return
        val taskId = SyncPaths.extractTaskId(path) ?: return

        val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
        val dataMap = dataMapItem.dataMap

        val opId = "task_update_${taskId}_${dataMap.getLong(DataMapKeys.KEY_TIMESTAMP)}"

        // Read phoneId explicitly if provided, otherwise try to parse from taskId
        val phoneId = dataMap.getLong("phoneId").takeIf { it > 0 } ?: taskId.toLongOrNull()

        try {
            syncRepository.applyIncomingTaskWithPhoneId(
                opId = opId,
                taskId = taskId,
                phoneId = phoneId,
                title = dataMap.getString(DataMapKeys.KEY_TITLE),
                titleUpdatedAt = dataMap.getLong(DataMapKeys.KEY_TITLE_UPDATED_AT).takeIf { it > 0 },
                notes = dataMap.getString(DataMapKeys.KEY_NOTES),
                notesUpdatedAt = dataMap.getLong(DataMapKeys.KEY_NOTES_UPDATED_AT).takeIf { it > 0 },
                completed = if (dataMap.containsKey(DataMapKeys.KEY_COMPLETED)) dataMap.getBoolean(DataMapKeys.KEY_COMPLETED) else null,
                completedUpdatedAt = dataMap.getLong(DataMapKeys.KEY_COMPLETED_UPDATED_AT).takeIf { it > 0 },
                deleted = if (dataMap.containsKey(DataMapKeys.KEY_DELETED)) dataMap.getBoolean(DataMapKeys.KEY_DELETED) else null,
                priority = if (dataMap.containsKey(DataMapKeys.KEY_PRIORITY)) dataMap.getInt(DataMapKeys.KEY_PRIORITY) else null,
                dueDate = dataMap.getLong(DataMapKeys.KEY_DUE_DATE).takeIf { it > 0 },
            )
            Timber.d("Applied task update for task $taskId (phoneId: $phoneId)")
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply task update for $taskId")
        }
    }

    /**
     * Handle full snapshot from phone.
     */
    private suspend fun handleSnapshot(event: DataEvent) {
        val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
        val dataMap = dataMapItem.dataMap

        val taskCount = dataMap.getInt(DataMapKeys.KEY_TASK_COUNT, 0)
        val snapshotTimestamp = dataMap.getLong(DataMapKeys.KEY_SNAPSHOT_TIMESTAMP, 0)

        Timber.d("Received snapshot with $taskCount tasks at $snapshotTimestamp")

        // Tasks are serialized as a list of DataMaps
        // This is a simplified implementation - in practice you might use Asset for large data
        val tasksData = mutableListOf<TaskSnapshotData>()

        for (i in 0 until taskCount) {
            val prefix = "task_$i"
            val taskId = dataMap.getString("${prefix}_id") ?: continue

            tasksData.add(TaskSnapshotData(
                taskId = taskId,
                title = dataMap.getString("${prefix}_title"),
                titleUpdatedAt = dataMap.getLong("${prefix}_titleUpdatedAt").takeIf { it > 0 },
                notes = dataMap.getString("${prefix}_notes"),
                notesUpdatedAt = dataMap.getLong("${prefix}_notesUpdatedAt").takeIf { it > 0 },
                completed = if (dataMap.containsKey("${prefix}_completed")) dataMap.getBoolean("${prefix}_completed") else null,
                completedUpdatedAt = dataMap.getLong("${prefix}_completedUpdatedAt").takeIf { it > 0 },
                deleted = if (dataMap.containsKey("${prefix}_deleted")) dataMap.getBoolean("${prefix}_deleted") else null,
                priority = if (dataMap.containsKey("${prefix}_priority")) dataMap.getInt("${prefix}_priority") else null,
                phoneId = dataMap.getLong("${prefix}_phoneId").takeIf { it > 0 },
                dueDate = dataMap.getLong("${prefix}_dueDate").takeIf { it > 0 },
            ))
        }

        syncRepository.applySnapshot(tasksData)
    }

    private fun handleDataDeleted(event: DataEvent) {
        val path = event.dataItem.uri.path
        Timber.d("Data deleted: $path")
        // Usually we don't need to do anything when DataItems are deleted
        // since deletion typically happens after processing
    }

    /**
     * Send acknowledgment to phone for their operation.
     */
    private suspend fun sendPhoneAck(opId: String, success: Boolean, error: String? = null) {
        val path = SyncPaths.phoneAckPath(opId)
        val request = PutDataMapRequest.create(path).apply {
            dataMap.putString(DataMapKeys.KEY_OP_ID, opId)
            dataMap.putBoolean(DataMapKeys.KEY_SUCCESS, success)
            if (error != null) {
                dataMap.putString(DataMapKeys.KEY_ERROR, error)
            }
            dataMap.putLong(DataMapKeys.KEY_TIMESTAMP, System.currentTimeMillis())
        }

        // Acks should be urgent
        request.setUrgent()

        val putDataRequest = request.asPutDataRequest()
        try {
            dataClient.putDataItem(putDataRequest).await()
            Timber.d("Sent ack for phone operation $opId (success=$success)")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send ack for phone operation $opId")
        }
    }

    /**
     * Request a full sync from phone.
     * Uses a unique nonce to ensure the DataItem is considered "changed" each time.
     */
    suspend fun requestSync() {
        val path = "/sync/request"
        val timestamp = System.currentTimeMillis()
        val request = PutDataMapRequest.create(path).apply {
            dataMap.putLong(DataMapKeys.KEY_TIMESTAMP, timestamp)
            // Add a unique nonce to ensure data is always different
            dataMap.putLong("nonce", System.nanoTime())
            setUrgent()
        }

        val putDataRequest = request.asPutDataRequest()
        try {
            dataClient.putDataItem(putDataRequest).await()
            Timber.d("Requested sync from phone (timestamp=$timestamp)")
        } catch (e: Exception) {
            Timber.e(e, "Failed to request sync")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: DataLayerSyncManager? = null

        fun getInstance(context: Context): DataLayerSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataLayerSyncManager(
                    context,
                    SyncRepository.getInstance(context)
                ).also { INSTANCE = it }
            }
        }
    }
}

/**
 * Sync state enum.
 */
enum class SyncState {
    IDLE,
    SYNCING,
    ERROR,
}
