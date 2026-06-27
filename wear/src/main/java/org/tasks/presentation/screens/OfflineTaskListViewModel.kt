/**
 * OfflineTaskListViewModel.kt — ViewModel for the main task list screen.
 *
 * ## Responsibilities
 * - Loads tasks from the local Room database via [TaskRepository].
 * - Polls phone connectivity every 5 seconds via [NodeClient].
 * - Monitors [DataLayerSyncManager.syncState] for syncing progress.
 * - Combines all flows into a single [OfflineTaskListState] exposed as [viewState].
 * - Provides actions: [toggleComplete], [refresh].
 *
 * ## Data flow
 * ```
 * Room DB (Flow<List<TaskLite>>)  ─┐
 * isConnected (MutableStateFlow)  ─┤── combine ──▶ OfflineTaskListState
 * syncState (StateFlow<SyncState>) ─┘
 * ```
 *
 * When [refresh] is called the ViewModel:
 * 1. Sends pending outbox operations to the phone.
 * 2. Requests a full snapshot from the phone.
 */
package org.tasks.presentation.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.tasks.data.local.SettingsEntity
import org.tasks.data.local.SettingsRepository
import org.tasks.data.local.TaskRepository
import org.tasks.data.sync.DataLayerSyncManager
import org.tasks.data.sync.SyncState
import org.tasks.presentation.model.TaskLite
import timber.log.Timber

/**
 * UI state for the offline-capable task list screen.
 */
data class OfflineTaskListState(
    val isLoading: Boolean = true,
    val tasks: List<TaskLite> = emptyList(),
    val settings: SettingsEntity = SettingsEntity(),
    val isConnected: Boolean = false,
    val syncState: SyncState = SyncState.IDLE,
    val hasPendingSync: Boolean = false,
)

/**
 * ViewModel for task list that works fully offline.
 * Reads tasks from local Room database and syncs when connected.
 */
class OfflineTaskListViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val taskRepository = TaskRepository.getInstance(application)
    private val settingsRepository = SettingsRepository.getInstance(application)
    private val syncManager = DataLayerSyncManager.getInstance(application)
    private val nodeClient: NodeClient = Wearable.getNodeClient(application)

    private val _isLoading = MutableStateFlow(true)
    private val _isConnected = MutableStateFlow(false)
    private val _hasPendingSync = MutableStateFlow(false)

    val viewState: StateFlow<OfflineTaskListState> = combine(
        taskRepository.getTasks(),
        settingsRepository.getSettings(),
        syncManager.syncState,
        _isConnected,
        _isLoading,
    ) { tasks, settings, syncState, connected, loading ->
        // Apply filters based on settings
        val filteredTasks = filterTasks(tasks, settings)

        OfflineTaskListState(
            isLoading = loading,
            tasks = filteredTasks,
            settings = settings,
            isConnected = connected,
            syncState = syncState,
            hasPendingSync = syncState == SyncState.SYNCING,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OfflineTaskListState()
    )

    init {
        viewModelScope.launch {
            // Ensure settings and sample data exist
            settingsRepository.ensureSettingsExist()
            taskRepository.insertSampleDataIfEmpty()
            _isLoading.value = false

            // Try initial sync if connected
            try {
                val connectedNodes = nodeClient.connectedNodes.await()
                if (connectedNodes.isNotEmpty()) {
                    _isConnected.value = true
                    Timber.d("Initial connection detected, requesting sync")
                    syncManager.processPendingOps()
                    syncManager.requestSync()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to check initial connection")
            }
        }

        // Start listening for sync events
        syncManager.startListening()

        // Check connection status periodically
        checkConnectionStatus()
    }

    override fun onCleared() {
        super.onCleared()
        syncManager.stopListening()
    }

    /**
     * Filter tasks based on current settings.
     */
    private fun filterTasks(tasks: List<TaskLite>, settings: SettingsEntity): List<TaskLite> {
        return tasks.filter { task ->
            // Filter by completion status
            val showCompleted = settings.showCompleted || !task.completed

            // Filter by hidden status
            val showHidden = settings.showHidden || !task.hidden

            showCompleted && showHidden
        }
    }

    /**
     * Check if phone is connected using NodeClient.
     * This checks for any connected nodes (typically the paired phone).
     */
    private fun checkConnectionStatus() {
        viewModelScope.launch {
            // Check periodically
            while (true) {
                try {
                    val connectedNodes = nodeClient.connectedNodes.await()
                    val wasConnected = _isConnected.value
                    _isConnected.value = connectedNodes.isNotEmpty()
                    Timber.d("Phone connection status: ${_isConnected.value} (${connectedNodes.size} nodes)")

                    // If just connected, sync pending operations and request full sync
                    if (_isConnected.value && !wasConnected) {
                        Timber.d("Connection restored, syncing with phone")
                        syncManager.processPendingOps()
                        syncManager.requestSync()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to check connection status")
                    _isConnected.value = false
                }

                // Check every 5 seconds
                delay(5000)
            }
        }
    }

    /**
     * Toggle task completion (works offline).
     */
    fun toggleComplete(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            taskRepository.toggleComplete(taskId, completed)
            trySyncPendingOps()
        }
    }

    /**
     * Delete a task (works offline).
     */
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
            trySyncPendingOps()
        }
    }

    /**
     * Toggle group collapsed state (works offline).
     */
    fun toggleGroup(groupId: Long, collapsed: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleGroupCollapsed(groupId, collapsed)
        }
    }

    /**
     * Try to sync pending operations if connected.
     */
    private suspend fun trySyncPendingOps() {
        if (_isConnected.value) {
            try {
                syncManager.processPendingOps()
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync pending operations")
            }
        }
    }

    /**
     * Force refresh: check connection, sync pending ops, and request full sync from phone.
     */
    fun refresh() {
        viewModelScope.launch {
            // First check connection status
            try {
                val connectedNodes = nodeClient.connectedNodes.await()
                _isConnected.value = connectedNodes.isNotEmpty()
                Timber.d("Refresh: connection status = ${_isConnected.value}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to check connection status")
                _isConnected.value = false
            }

            // If connected, sync pending ops and request full sync
            if (_isConnected.value) {
                try {
                    // Process any pending outbox operations
                    syncManager.processPendingOps()
                    // Request full sync from phone
                    syncManager.requestSync()
                    Timber.d("Refresh: requested sync from phone")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync")
                }
            }
        }
    }

    /**
     * Request a full sync from phone.
     */
    fun requestSync() {
        viewModelScope.launch {
            try {
                syncManager.requestSync()
            } catch (e: Exception) {
                Timber.e(e, "Failed to request sync")
            }
        }
    }
}
