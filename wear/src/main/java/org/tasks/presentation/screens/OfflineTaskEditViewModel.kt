/**
 * OfflineTaskEditViewModel.kt — ViewModel for creating and editing a single task.
 *
 * ## Lifecycle
 * - Created by [OfflineTaskEditViewModelFactory] with an optional [taskId].
 * - If [taskId] is non-null, loads the existing task from Room on init.
 * - Otherwise initialises a blank [OfflineEditUiState] for a new task.
 *
 * ## State
 * Exposes a single [uiState] ([StateFlow]) that drives [OfflineTaskEditScreen].
 * All mutations go through dedicated setter methods ([setTitle], [setNotes],
 * [setDueDate], etc.) which call `_uiState.update { … }`.
 *
 * ## Saving
 * [save] writes the task to Room via [TaskRepository.saveTask].
 * The repository handles creating the outbox operation for sync.
 * After saving, the ViewModel calls the provided [onComplete] callback
 * (which navigates back in [MainActivity]).
 *
 * ## Connectivity
 * Polls [NodeClient] every 5 s so the edit screen can show "Connected"
 * or "Save (offline)" on the save button.
 *
 * ## Factory
 * [OfflineTaskEditViewModelFactory] is needed because the ViewModel
 * takes a constructor parameter ([taskId]) in addition to [Application].
 */
package org.tasks.presentation.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.tasks.data.local.TaskRepository
import org.tasks.data.sync.DataLayerSyncManager
import timber.log.Timber

/**
 * UI state for the offline task edit screen.
 */
data class OfflineEditUiState(
    val isLoading: Boolean = true,
    val taskId: String? = null,
    val title: String = "",
    val notes: String = "",
    val completed: Boolean = false,
    val priority: Int = 0,
    val repeating: Boolean = false,
    val isSaving: Boolean = false,
    val isConnected: Boolean = false,
    // Due date and time
    val dueDate: Long? = null,
    val dueTime: Long? = null,
    // Reminder
    val reminder: Boolean = false,
    val reminderTime: Long? = null,
    // UI state for date/time pickers
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
)

/**
 * ViewModel for task editing that works fully offline.
 * Saves to local Room database and syncs when connected.
 */
class OfflineTaskEditViewModel(
    application: Application,
    private val taskId: String?,
) : AndroidViewModel(application) {

    private val taskRepository = TaskRepository.getInstance(application)
    private val syncManager = DataLayerSyncManager.getInstance(application)
    private val nodeClient: NodeClient = Wearable.getNodeClient(application)

    private val _uiState = MutableStateFlow(OfflineEditUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (!taskId.isNullOrEmpty()) {
            loadTask(taskId)
        } else {
            // New task mode
            _uiState.update {
                it.copy(isLoading = false, taskId = null)
            }
        }
        checkConnectionStatus()
    }

    /**
     * Load task from local database.
     */
    private fun loadTask(id: String) {
        viewModelScope.launch {
            val task = taskRepository.getTask(id)
            _uiState.update { state ->
                if (task != null) {
                    state.copy(
                        isLoading = false,
                        taskId = id,
                        title = task.title,
                        notes = task.notes,
                        completed = task.completed,
                        priority = task.priority,
                        repeating = task.repeating,
                        dueDate = task.dueDate,
                        dueTime = task.dueTime,
                        reminder = task.reminder,
                        reminderTime = task.reminderTime,
                    )
                } else {
                    // Task not found, treat as new task
                    state.copy(isLoading = false, taskId = null)
                }
            }
        }
    }

    /**
     * Check if phone is connected using NodeClient.
     */
    private fun checkConnectionStatus() {
        viewModelScope.launch {
            try {
                val connectedNodes = nodeClient.connectedNodes.await()
                _uiState.update { it.copy(isConnected = connectedNodes.isNotEmpty()) }
                Timber.d("Edit screen connection status: ${connectedNodes.isNotEmpty()}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to check connection status")
                _uiState.update { it.copy(isConnected = false) }
            }
        }
    }

    /**
     * Update the title.
     */
    fun setTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    /**
     * Update the notes.
     */
    fun setNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    /**
     * Toggle completion status.
     */
    fun toggleCompleted() {
        _uiState.update { it.copy(completed = !it.completed) }
    }

    /**
     * Set priority.
     */
    fun setPriority(priority: Int) {
        _uiState.update { it.copy(priority = priority) }
    }

    /**
     * Set due date.
     */
    fun setDueDate(dueDate: Long?) {
        _uiState.update { it.copy(dueDate = dueDate) }
    }

    /**
     * Set due time.
     */
    fun setDueTime(dueTime: Long?) {
        _uiState.update { it.copy(dueTime = dueTime) }
    }

    /**
     * Toggle reminder.
     */
    fun toggleReminder() {
        _uiState.update { state ->
            val newReminder = !state.reminder
            state.copy(
                reminder = newReminder,
                // If reminder is enabled and no reminder time is set, use due date/time
                reminderTime = if (newReminder && state.reminderTime == null) {
                    state.dueDate ?: state.dueTime
                } else state.reminderTime
            )
        }
    }

    /**
     * Set reminder time.
     */
    fun setReminderTime(reminderTime: Long?) {
        _uiState.update { it.copy(reminderTime = reminderTime) }
    }

    /**
     * Show/hide date picker.
     */
    fun showDatePicker(show: Boolean) {
        _uiState.update { it.copy(showDatePicker = show) }
    }

    /**
     * Show/hide time picker.
     */
    fun showTimePicker(show: Boolean) {
        _uiState.update { it.copy(showTimePicker = show) }
    }

    /**
     * Clear due date.
     */
    fun clearDueDate() {
        _uiState.update { it.copy(dueDate = null, dueTime = null) }
    }

    /**
     * Save the task to local database.
     * Syncs automatically when phone is connected.
     * @param onComplete callback when save is complete
     */
    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val state = _uiState.value
            val savedId = taskRepository.saveTask(
                id = state.taskId,
                title = state.title.ifEmpty { "Untitled Task" },
                notes = state.notes,
                dueDate = state.dueDate,
                dueTime = state.dueTime,
                reminder = state.reminder,
                reminderTime = state.reminderTime,
            )

            // Update completion status if changed
            if (state.taskId != null) {
                taskRepository.toggleComplete(savedId, state.completed)
            } else if (state.completed) {
                taskRepository.toggleComplete(savedId, true)
            }

            // Try to sync if connected
            if (_uiState.value.isConnected) {
                try {
                    syncManager.processPendingOps()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync after save")
                    // Task is saved locally, will sync later
                }
            }

            _uiState.update { it.copy(isSaving = false) }
            onComplete()
        }
    }

    /**
     * Delete the current task.
     */
    fun delete(onComplete: () -> Unit) {
        val id = _uiState.value.taskId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            taskRepository.deleteTask(id)

            // Try to sync if connected
            if (_uiState.value.isConnected) {
                try {
                    syncManager.processPendingOps()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync after delete")
                }
            }

            _uiState.update { it.copy(isSaving = false) }
            onComplete()
        }
    }
}

/**
 * Factory for creating OfflineTaskEditViewModel with taskId parameter.
 */
class OfflineTaskEditViewModelFactory(
    private val application: Application,
    private val taskId: String?,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OfflineTaskEditViewModel::class.java)) {
            return OfflineTaskEditViewModel(application, taskId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

