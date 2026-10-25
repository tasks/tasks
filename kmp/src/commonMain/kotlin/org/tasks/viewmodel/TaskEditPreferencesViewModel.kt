package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.preferences.TasksPreferences

/**
 * State and persistence for the desktop "Edit screen options" settings pane.
 *
 * Mirrors the native Android `TaskEditPreferencesViewModel` (same rows,
 * same defaults, same key semantics), but stores values in the shared
 * [TasksPreferences] DataStore instead of Android SharedPreferences.
 *
 * Only the "Back button saves task" row ships in this milestone. The other
 * native rows are omitted deliberately: multiline title is a no-op on
 * desktop (the title field already grows across lines), the comments row
 * has no desktop comments UI to toggle, the "customize" row is the legacy
 * BeastMode screen, and "without unlock" is Android-only. See
 * SPEC-settings-panes.md §3.5 for the omission table.
 */
class TaskEditPreferencesViewModel(
    private val tasksPreferences: TasksPreferences,
) : ViewModel() {

    data class State(
        val loaded: Boolean = false,
        val backButtonSaves: Boolean = DEFAULT_BACK_BUTTON_SAVES,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = State(
                loaded = true,
                backButtonSaves = tasksPreferences.get(
                    TasksPreferences.taskEditBackButtonSaves,
                    DEFAULT_BACK_BUTTON_SAVES,
                ),
            )
        }
    }

    fun updateBackButtonSaves(enabled: Boolean) {
        _state.update { it.copy(backButtonSaves = enabled) }
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.taskEditBackButtonSaves, enabled)
        }
    }

    companion object {
        // Native default is false; the desktop seeds true on first launch (see desktopMain
        // main.kt) so the existing Escape-to-save behaviour is the de-facto desktop default.
        const val DEFAULT_BACK_BUTTON_SAVES = false
    }
}
