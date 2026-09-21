package org.tasks.dialogs

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.LocalBroadcastManager
import org.tasks.billing.PurchaseState
import org.tasks.broadcast.ComposeRefreshBroadcaster
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.compose.FilterSelectionActivity.Companion.EXTRA_LISTS_ONLY
import org.tasks.data.dao.CaldavDao
import org.tasks.filters.Filter
import org.tasks.filters.FilterProvider
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.preferences.TasksPreferences
import org.tasks.viewmodel.FilterPickerViewModel
import org.tasks.widget.WidgetPreferences
import javax.inject.Inject

@HiltViewModel
class FilterPickerHiltViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    filterProvider: FilterProvider,
    caldavDao: CaldavDao,
    tasksPreferences: TasksPreferences,
    purchaseState: PurchaseState,
    refreshBroadcaster: RefreshBroadcaster,
    composeRefreshBroadcaster: ComposeRefreshBroadcaster,
    private val preferences: Preferences,
    private val defaultFilterProvider: DefaultFilterProvider,
    private val localBroadcastManager: LocalBroadcastManager,
) : FilterPickerViewModel(
    filterProvider = filterProvider,
    caldavDao = caldavDao,
    tasksPreferences = tasksPreferences,
    purchaseState = purchaseState,
    refreshBroadcaster = refreshBroadcaster,
    listsOnly = savedStateHandle[EXTRA_LISTS_ONLY] ?: false,
    refreshFlow = composeRefreshBroadcaster.refreshes,
) {
    fun updateWidget(widgetId: Int, filter: Filter) {
        WidgetPreferences(context, preferences, widgetId)
            .setFilter(defaultFilterProvider.getFilterPreferenceValue(filter))
        localBroadcastManager.reconfigureWidget(widgetId)
    }
}
