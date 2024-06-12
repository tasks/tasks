package org.tasks.dialogs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.compose.FilterSelectionActivity
import org.tasks.data.dao.CaldavDao
import org.tasks.filters.Filter
import org.tasks.filters.FilterListItem
import org.tasks.filters.FilterProvider
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import org.tasks.themes.CustomIcons
import javax.inject.Inject

@HiltViewModel
class FilterPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val filterProvider: FilterProvider,
    private val localBroadcastManager: LocalBroadcastManager,
    private val inventory: Inventory,
    private val colorProvider: ColorProvider,
    private val preferences: Preferences,
    private val caldavDao: CaldavDao,
) : ViewModel() {
    private val listsOnly = savedStateHandle[FilterSelectionActivity.EXTRA_LISTS_ONLY] ?: false

    data class ViewState(
        val filters: List<FilterListItem> = emptyList(),
        val allFilters: List<Filter> = emptyList(),
        val searchResults: List<Filter> = emptyList(),
        val query: String = "",
    )

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState>
        get() = _viewState.asStateFlow()

    private val refreshReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    private fun refresh() = viewModelScope.launch {
        _viewState.update { state ->
            state.copy(
                filters = if (listsOnly) {
                    filterProvider.listPickerItems().filterNot { it is Filter && it.isReadOnly }
                } else {
                    filterProvider.filterPickerItems()
                },
                allFilters = if (listsOnly) {
                    filterProvider.allLists().filterNot { it.isReadOnly }
                } else {
                    filterProvider.allFilters()
                },
            )
        }
    }

    fun onClick(subheader: NavigationDrawerSubheader) = viewModelScope.launch {
        val collapsed = !subheader.isCollapsed
        when (subheader.subheaderType) {
            NavigationDrawerSubheader.SubheaderType.PREFERENCE ->
                preferences.setBoolean(subheader.id.toInt(), collapsed)
            NavigationDrawerSubheader.SubheaderType.GOOGLE_TASKS,
            NavigationDrawerSubheader.SubheaderType.CALDAV,
            NavigationDrawerSubheader.SubheaderType.TASKS,
            NavigationDrawerSubheader.SubheaderType.ETESYNC ->
                caldavDao.setCollapsed(subheader.id, collapsed)

            else -> throw IllegalStateException()
        }
        localBroadcastManager.broadcastRefreshList()
    }

    fun getIcon(filter: Filter): Int {
        if (filter.icon < 1000 || inventory.hasPro) {
            val icon = CustomIcons.getIconResId(filter.icon)
            if (icon != null) {
                return icon
            }
        }
        return R.drawable.ic_list_24px
    }

    fun getColor(filter: Filter): Int {
        if (filter.tint != 0) {
            val color = colorProvider.getThemeColor(filter.tint, true)
            if (color.isFree || inventory.purchasedThemes()) {
                return color.primaryColor
            }
        }
        return context.getColor(R.color.text_primary)
    }

    override fun onCleared() {
        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }

    fun onQueryChange(query: String) {
        _viewState.update { state ->
            state.copy(
                query = query,
                searchResults = state.allFilters
                    .filter { it.title!!.contains(query, ignoreCase = true) }
                    .sortedBy { it.title },
            )
        }
    }

    init {
        localBroadcastManager.registerRefreshListReceiver(refreshReceiver)
        refresh()
    }
}