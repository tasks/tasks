package com.todoroo.astrid.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoroo.astrid.activity.MainActivity.Companion.LOAD_FILTER
import com.todoroo.astrid.activity.MainActivity.Companion.OPEN_FILTER
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.CustomFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.Filter.Companion.NO_COUNT
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.api.TagFilter
import com.todoroo.astrid.data.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Tasks.Companion.IS_GENERIC
import org.tasks.billing.Inventory
import org.tasks.compose.drawer.DrawerItem
import org.tasks.data.CaldavDao
import org.tasks.data.TaskDao
import org.tasks.filters.FilterProvider
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.PlaceFilter
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import org.tasks.themes.CustomIcons
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val defaultFilterProvider: DefaultFilterProvider,
    private val filterProvider: FilterProvider,
    private val taskDao: TaskDao,
    private val localBroadcastManager: LocalBroadcastManager,
    private val inventory: Inventory,
    private val colorProvider: ColorProvider,
    private val caldavDao: CaldavDao,
    private val preferences: Preferences,
) : ViewModel() {

    data class State(
        val begForMoney: Boolean = false,
        val filter: Filter,
        val task: Task? = null,
        val drawerOpen: Boolean = false,
        val drawerItems: ImmutableList<DrawerItem> = persistentListOf(),
    )

    private val _state = MutableStateFlow(
        State(
            filter = savedStateHandle.get<Filter>(OPEN_FILTER)
                ?: savedStateHandle.get<String>(LOAD_FILTER)?.let {
                    runBlocking { defaultFilterProvider.getFilterFromPreference(it) }
                }
                ?: runBlocking { defaultFilterProvider.getStartupFilter() },
            begForMoney = if (IS_GENERIC) !inventory.hasTasksAccount else !inventory.hasPro,
        )
    )
    val state = _state.asStateFlow()

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                LocalBroadcastManager.REFRESH,
                LocalBroadcastManager.REFRESH_LIST -> updateFilters()
            }
        }
    }

    fun setFilter(
        filter: Filter,
        task: Task? = null,
    ) {
        _state.update {
            it.copy(
                filter = filter,
                task = task,
            )
        }
        updateFilters()
        defaultFilterProvider.setLastViewedFilter(filter)
    }

    fun setDrawerOpen(open: Boolean) {
        _state.update { it.copy(drawerOpen = open) }
    }

    init {
        localBroadcastManager.registerRefreshListReceiver(refreshReceiver)
        updateFilters()
    }

    override fun onCleared() {
        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }

    fun updateFilters() = viewModelScope.launch(Dispatchers.Default) {
        val selected = state.value.filter
        filterProvider
            .drawerItems()
            .map { item ->
                when (item) {
                    is Filter ->
                        DrawerItem.Filter(
                            title = item.title ?: "",
                            icon = getIcon(item),
                            color = getColor(item),
                            count = item.count.takeIf { it != NO_COUNT } ?: try {
                                taskDao.count(item)
                            } catch (e: Exception) {
                                Timber.e(e)
                                0
                            },
                            selected = item.areItemsTheSame(selected),
                            shareCount = if (item is CaldavFilter) item.principals else 0,
                            type = { item },
                        )
                    is NavigationDrawerSubheader ->
                        DrawerItem.Header(
                            title = item.title ?: "",
                            collapsed = item.isCollapsed,
                            hasError = item.error,
                            canAdd = item.addIntent != null,
                            type = { item },
                        )
                    else -> throw IllegalArgumentException()
                }
            }
            .let { filters -> _state.update { it.copy(drawerItems = filters.toPersistentList()) } }
    }

    private fun getColor(filter: Filter): Int {
        if (filter.tint != 0) {
            val color = colorProvider.getThemeColor(filter.tint, true)
            if (color.isFree || inventory.purchasedThemes()) {
                return color.primaryColor
            }
        }
        return 0
    }

    private fun getIcon(filter: Filter): Int {
        if (filter.icon < 1000 || filter.icon == CustomIcons.PLACE || inventory.hasPro) {
            val icon = CustomIcons.getIconResId(filter.icon)
            if (icon != null) {
                return icon
            }
        }
        return when (filter) {
            is TagFilter -> R.drawable.ic_outline_label_24px
            is GtasksFilter,
            is CaldavFilter -> R.drawable.ic_list_24px

            is CustomFilter -> R.drawable.ic_outline_filter_list_24px
            is PlaceFilter -> R.drawable.ic_outline_place_24px
            else -> filter.icon
        }
    }

    fun toggleCollapsed(subheader: NavigationDrawerSubheader) = viewModelScope.launch {
        val collapsed = !subheader.isCollapsed
        when (subheader.subheaderType) {
            NavigationDrawerSubheader.SubheaderType.PREFERENCE -> {
                preferences.setBoolean(subheader.id.toInt(), collapsed)
                localBroadcastManager.broadcastRefreshList()
            }
            NavigationDrawerSubheader.SubheaderType.GOOGLE_TASKS,
            NavigationDrawerSubheader.SubheaderType.CALDAV,
            NavigationDrawerSubheader.SubheaderType.TASKS,
            NavigationDrawerSubheader.SubheaderType.ETESYNC -> {
                caldavDao.setCollapsed(subheader.id, collapsed)
                localBroadcastManager.broadcastRefreshList()
            }
        }
    }

    fun setTask(task: Task?) {
        _state.update { it.copy(task = task) }
    }
}
