package org.tasks.selection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Mirrors the id of the task the user currently has open in the task edit pane,
 * so code outside the composition (the in-app MCP server) can answer
 * "which task is the user looking at right now?"
 *
 * The value is only ever written by the App root, derived from the navigation
 * back stack: opening a task sets the id, closing the edit pane clears it to
 * null.
 */
class SelectedTaskStore {
    private val _selectedTaskId = MutableStateFlow<Long?>(null)

    val selectedTaskId: StateFlow<Long?> = _selectedTaskId

    fun update(taskId: Long?) {
        _selectedTaskId.value = taskId
    }
}
