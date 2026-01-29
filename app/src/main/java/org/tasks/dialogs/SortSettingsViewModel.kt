package org.tasks.dialogs

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.todoroo.astrid.core.SortHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.dialogs.SortSettingsActivity.Companion.EXTRA_FILTER_KEY
import org.tasks.dialogs.SortSettingsActivity.Companion.WIDGET_NONE
import org.tasks.preferences.FilterPreferences
import org.tasks.preferences.Preferences
import org.tasks.tasklist.SectionedDataSource.Companion.HEADER_COMPLETED
import org.tasks.widget.WidgetPreferences
import javax.inject.Inject

@HiltViewModel
class SortSettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    private val appPreferences: Preferences,
    private val firebase: Firebase,
): ViewModel() {
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
    private val widgetId = savedStateHandle[SortSettingsActivity.EXTRA_WIDGET_ID] ?: WIDGET_NONE
    private val filterKey = savedStateHandle.get<String>(EXTRA_FILTER_KEY)
    private val preferences =
        widgetId
            .takeIf { it != WIDGET_NONE }
            ?.let { WidgetPreferences(context, appPreferences, it) }
            ?: filterKey
                ?.let { FilterPreferences(appPreferences, it) }
            ?: appPreferences

    private fun trackSortChange(setting: String, value: Any) {
        firebase.logEvent(
            R.string.event_sort_change,
            R.string.param_type to setting,
            R.string.param_value to value.toString()
        )
    }

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

    fun setGroupMode(groupMode: Int) {
        if (preferences.groupMode == groupMode) {
            return
        }
        trackSortChange("group", groupMode.toSortName())
        if (groupMode != SortHelper.GROUP_NONE) {
            preferences.isManualSort = false
            preferences.isAstridSort = false
            if (preferences is WidgetPreferences) {
                preferences.collapsed = setOf(HEADER_COMPLETED)
            }
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
                subtaskAscending = ascending
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

    val forceReload: Boolean
        get() = initialState.manualSort != _viewState.value.manualSort
                || initialState.astridSort != _viewState.value.astridSort

    val changedGroup: Boolean
        get() = initialState.groupMode != _viewState.value.groupMode

    private fun Int.toSortName(): String = when (this) {
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
