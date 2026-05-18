package org.tasks.viewmodel

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.billing.PurchaseState
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.compose.throttleLatest
import org.tasks.data.dao.CaldavDao
import org.tasks.filters.Filter
import org.tasks.filters.FilterListItem
import org.tasks.filters.FilterProvider
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.getIcon
import org.tasks.kmp.org.tasks.themes.ColorProvider
import org.tasks.preferences.TasksPreferences

open class FilterPickerViewModel(
    private val filterProvider: FilterProvider,
    private val caldavDao: CaldavDao,
    private val tasksPreferences: TasksPreferences,
    private val purchaseState: PurchaseState,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val listsOnly: Boolean = false,
    refreshFlow: Flow<Unit> = emptyFlow(),
) : ViewModel() {

    data class ViewState(
        val filters: List<FilterListItem> = emptyList(),
        val allFilters: List<Filter> = emptyList(),
        val searchResults: List<Filter> = emptyList(),
        val query: String = "",
    )

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState>
        get() = _viewState.asStateFlow()

    init {
        refresh()

        refreshFlow
            .throttleLatest(1000)
            .onEach { refresh() }
            .launchIn(viewModelScope)
    }

    private fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
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
    }

    fun onClick(subheader: NavigationDrawerSubheader) {
        viewModelScope.launch(Dispatchers.IO) {
            val collapsed = !subheader.isCollapsed
            when (subheader.subheaderType) {
                NavigationDrawerSubheader.SubheaderType.PREFERENCE ->
                    tasksPreferences.set(booleanPreferencesKey(subheader.id), collapsed)

                NavigationDrawerSubheader.SubheaderType.CALDAV,
                NavigationDrawerSubheader.SubheaderType.TASKS ->
                    caldavDao.setCollapsed(subheader.id, collapsed)
            }
            refreshBroadcaster.broadcastRefresh()
        }
    }

    fun getIcon(filter: Filter): String? = filter.getIcon(purchaseState)

    fun getColor(tint: Int, isDark: Boolean): Int? {
        if (tint == 0) return null
        if (!ColorProvider.isFreeColor(tint) && !purchaseState.purchasedThemes()) return null
        return ColorProvider.getColor(tint, isDark, adjust = true)
    }

    fun onQueryChange(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _viewState.update { state ->
                state.copy(
                    query = query,
                    searchResults = state.allFilters
                        .filter { it.title.contains(query, ignoreCase = true) }
                        .sortedBy { it.title },
                )
            }
        }
    }
}
