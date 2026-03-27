package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import com.todoroo.astrid.core.SortHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.preferences.QueryPreferences

open class SortSettingsViewModel(
    private val preferences: QueryPreferences,
    private val reporting: Reporting,
) : ViewModel() {
    data class ViewState(
        val manualSort: Boolean,
        val astridSort: Boolean,
        val groupMode: Int,
        val groupAscending: Boolean,
        val completedAtBottom: Boolean,
        val sortMode: Int,
        val sortAscending: Boolean,
        val completedMode: Int,
        val completedAscending: Boolean,
        val subtaskMode: Int,
        val subtaskAscending: Boolean,
    )

    private val initialState = ViewState(
        manualSort = preferences.isManualSort,
        astridSort = preferences.isAstridSort,
        groupMode = preferences.groupMode,
        groupAscending = preferences.groupAscending,
        completedMode = preferences.completedMode,
        completedAscending = preferences.completedAscending,
        completedAtBottom = preferences.completedTasksAtBottom,
        sortMode = preferences.sortMode,
        sortAscending = preferences.sortAscending,
        subtaskMode = preferences.subtaskMode,
        subtaskAscending = preferences.subtaskAscending,
    )
    private val _viewState = MutableStateFlow(initialState)
    val state = _viewState.asStateFlow()

    val forceReload: Boolean
        get() = initialState.manualSort != _viewState.value.manualSort
                || initialState.astridSort != _viewState.value.astridSort

    val changedGroup: Boolean
        get() = initialState.groupMode != _viewState.value.groupMode

    fun setSortAscending(ascending: Boolean) {
        preferences.sortAscending = ascending
        _viewState.update { it.copy(sortAscending = ascending) }
    }

    fun setGroupAscending(ascending: Boolean) {
        preferences.groupAscending = ascending
        _viewState.update { it.copy(groupAscending = ascending) }
    }

    fun setCompletedAscending(ascending: Boolean) {
        preferences.completedAscending = ascending
        _viewState.update { it.copy(completedAscending = ascending) }
    }

    fun setSubtaskAscending(ascending: Boolean) {
        preferences.subtaskAscending = ascending
        _viewState.update { it.copy(subtaskAscending = ascending) }
    }

    fun setCompletedAtBottom(completedAtBottom: Boolean) {
        preferences.completedTasksAtBottom = completedAtBottom
        _viewState.update { it.copy(completedAtBottom = completedAtBottom) }
    }

    open fun setGroupMode(groupMode: Int) {
        if (preferences.groupMode == groupMode) {
            return
        }
        trackSortChange("group", groupMode.toSortName())
        if (groupMode != SortHelper.GROUP_NONE) {
            preferences.isManualSort = false
            preferences.isAstridSort = false
        }
        preferences.groupMode = groupMode
        val ascending = when (groupMode) {
            SortHelper.SORT_MODIFIED,
            SortHelper.SORT_CREATED -> false
            else -> true
        }
        preferences.groupAscending = ascending
        _viewState.update {
            it.copy(
                manualSort = preferences.isManualSort,
                astridSort = preferences.isAstridSort,
                groupMode = groupMode,
                groupAscending = ascending,
            )
        }
    }

    fun setCompletedMode(completedMode: Int) {
        preferences.completedMode = completedMode
        val ascending = when (completedMode) {
            SortHelper.SORT_COMPLETED,
            SortHelper.SORT_MODIFIED,
            SortHelper.SORT_CREATED -> false
            else -> true
        }
        preferences.completedAscending = ascending
        _viewState.update {
            it.copy(
                completedMode = completedMode,
                completedAscending = ascending,
            )
        }
    }

    fun setSortMode(sortMode: Int) {
        trackSortChange("sort", sortMode.toSortName())
        preferences.isManualSort = false
        preferences.isAstridSort = false
        preferences.sortMode = sortMode
        val ascending = when (sortMode) {
            SortHelper.SORT_MODIFIED,
            SortHelper.SORT_CREATED -> false
            else -> true
        }
        preferences.sortAscending = ascending
        _viewState.update {
            it.copy(
                manualSort = false,
                astridSort = false,
                sortMode = sortMode,
                sortAscending = ascending,
            )
        }
    }

    fun setSubtaskMode(subtaskMode: Int) {
        trackSortChange("subtask", subtaskMode.toSortName())
        preferences.subtaskMode = subtaskMode
        val ascending = when (subtaskMode) {
            SortHelper.SORT_MODIFIED,
            SortHelper.SORT_CREATED -> false
            else -> true
        }
        preferences.subtaskAscending = ascending
        _viewState.update {
            it.copy(
                subtaskMode = subtaskMode,
                subtaskAscending = ascending,
            )
        }
    }

    fun setManual(value: Boolean) {
        trackSortChange("sort", "manual")
        preferences.isManualSort = value
        if (value) {
            preferences.groupMode = SortHelper.GROUP_NONE
        }
        _viewState.update {
            it.copy(
                groupMode = if (value) SortHelper.GROUP_NONE else it.groupMode,
                manualSort = value,
            )
        }
    }

    fun setAstrid(value: Boolean) {
        trackSortChange("sort", "astrid")
        preferences.isAstridSort = value
        if (value) {
            preferences.groupMode = SortHelper.GROUP_NONE
        }
        _viewState.update {
            it.copy(
                groupMode = if (value) SortHelper.GROUP_NONE else it.groupMode,
                astridSort = value,
            )
        }
    }

    private fun trackSortChange(setting: String, value: Any) {
        reporting.logEvent(
            AnalyticsEvents.SORT_CHANGE,
            AnalyticsEvents.PARAM_TYPE to setting,
            PARAM_VALUE to value.toString(),
        )
    }

    companion object {
        private const val PARAM_VALUE = "value"

        fun Int.toSortName(): String = when (this) {
            SortHelper.GROUP_NONE -> "none"
            SortHelper.SORT_ALPHA -> "alpha"
            SortHelper.SORT_DUE -> "due"
            SortHelper.SORT_IMPORTANCE -> "importance"
            SortHelper.SORT_MODIFIED -> "modified"
            SortHelper.SORT_CREATED -> "created"
            SortHelper.SORT_START -> "start"
            SortHelper.SORT_LIST -> "list"
            SortHelper.SORT_COMPLETED -> "completed"
            SortHelper.SORT_MANUAL -> "manual"
            SortHelper.SORT_AUTO -> "auto"
            else -> "unknown"
        }
    }
}
