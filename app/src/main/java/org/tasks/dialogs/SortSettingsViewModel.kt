package org.tasks.dialogs

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.todoroo.astrid.core.SortHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.analytics.Firebase
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.dialogs.SortSettingsActivity.Companion.EXTRA_FILTER_KEY
import org.tasks.dialogs.SortSettingsActivity.Companion.WIDGET_NONE
import org.tasks.preferences.FilterPreferences
import org.tasks.preferences.Preferences
import org.tasks.preferences.TasksPreferences
import org.tasks.tasklist.SectionedDataSource.Companion.HEADER_COMPLETED
import org.tasks.widget.WidgetPreferences
import javax.inject.Inject

@HiltViewModel
class SortSettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    appPreferences: Preferences,
    tasksPreferences: TasksPreferences,
    firebase: Firebase,
    refreshBroadcaster: RefreshBroadcaster,
) : org.tasks.viewmodel.SortSettingsViewModel(
    preferences = savedStateHandle
        .get<Int>(SortSettingsActivity.EXTRA_WIDGET_ID)
        ?.takeIf { it != WIDGET_NONE }
        ?.let { WidgetPreferences(context, appPreferences, it) }
        ?: savedStateHandle.get<String>(EXTRA_FILTER_KEY)
            ?.takeIf { appPreferences.isPerListSortEnabled }
            ?.let { FilterPreferences(appPreferences, tasksPreferences, it) }
        ?: appPreferences,
    reporting = firebase,
    refreshBroadcaster = refreshBroadcaster,
) {
    private val widgetPreferences = savedStateHandle
        .get<Int>(SortSettingsActivity.EXTRA_WIDGET_ID)
        ?.takeIf { it != WIDGET_NONE }
        ?.let { WidgetPreferences(context, appPreferences, it) }

    override fun setGroupMode(groupMode: Int) {
        if (groupMode != SortHelper.GROUP_NONE) {
            widgetPreferences?.let { it.collapsed = setOf(HEADER_COMPLETED) }
        }
        super.setGroupMode(groupMode)
    }
}
