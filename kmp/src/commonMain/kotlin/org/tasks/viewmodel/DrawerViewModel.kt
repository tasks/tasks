package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.billing.PurchaseState
import org.tasks.compose.drawer.DrawerItem
import org.tasks.compose.throttleLatest
import org.tasks.data.NO_COUNT
import org.tasks.data.count
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TaskDao
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.FilterProvider
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.SignInPrompt
import org.tasks.preferences.TasksPreferences
import org.tasks.kmp.org.tasks.themes.ColorProvider

private const val TAG = "DrawerViewModel"

open class DrawerViewModel(
    private val filterProvider: FilterProvider,
    private val taskDao: TaskDao,
    private val caldavDao: CaldavDao,
    private val tasksPreferences: TasksPreferences,
    private val purchaseState: PurchaseState,
    refreshFlow: Flow<Unit> = emptyFlow(),
) : ViewModel() {

    data class State(
        val drawerItems: ImmutableList<DrawerItem> = persistentListOf(),
        val searchItems: ImmutableList<DrawerItem> = persistentListOf(),
        val menuQuery: String = "",
        val selectedFilter: Filter? = null,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _sidebarExpanded = MutableStateFlow(
        runBlocking { tasksPreferences.get(TasksPreferences.sidebarExpanded, true) }
    )
    val sidebarExpanded = _sidebarExpanded.asStateFlow()

    fun setSidebarExpanded(expanded: Boolean) {
        _sidebarExpanded.value = expanded
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.sidebarExpanded, expanded)
        }
    }

    init {
        updateFilters()

        refreshFlow
            .throttleLatest(1000)
            .onEach { updateFilters() }
            .launchIn(viewModelScope)
    }

    fun setSelectedFilter(filter: Filter?) {
        _state.update { it.copy(selectedFilter = filter) }
        updateFilters()
    }

    fun updateFilters() {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedFilter = _state.value.selectedFilter
            filterProvider
                .drawerItems()
                .map { item -> item.toDrawerItem(selectedFilter) }
                .let { filters ->
                    _state.update { it.copy(drawerItems = filters.toPersistentList()) }
                }
            val query = _state.value.menuQuery
            if (query.isNotBlank()) {
                updateSearch(query, selectedFilter)
            }
        }
    }

    fun setMenuQuery(query: String) {
        _state.update { it.copy(menuQuery = query) }
        if (query.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                updateSearch(query, _state.value.selectedFilter)
            }
        }
    }

    private suspend fun updateSearch(query: String, selectedFilter: Filter?) {
        filterProvider
            .allFilters()
            .filter { it.title.contains(query, ignoreCase = true) }
            .map { item ->
                DrawerItem.Filter(
                    title = item.title,
                    icon = item.resolveIcon(purchaseState),
                    color = if (purchaseState.purchasedThemes() || ColorProvider.isFreeColor(item.tint)) item.tint else 0,
                    adjustColor = item.tint != 0,
                    count = item.count.takeIf { it != NO_COUNT } ?: try {
                        taskDao.count(item)
                    } catch (e: Exception) {
                        Logger.e(e, tag = TAG) { "Failed to count tasks" }
                        0
                    },
                    selected = selectedFilter?.let { item.areItemsTheSame(it) } ?: false,
                    shareCount = if (item is CaldavFilter) item.principals else 0,
                    filter = item,
                )
            }
            .let { filters ->
                _state.update { it.copy(searchItems = filters.toPersistentList()) }
            }
    }

    fun toggleCollapsed(subheader: NavigationDrawerSubheader) {
        viewModelScope.launch(Dispatchers.IO) {
            toggleCollapsed(subheader, caldavDao, tasksPreferences)
            updateFilters()
        }
    }

    private suspend fun org.tasks.filters.FilterListItem.toDrawerItem(
        selectedFilter: Filter?,
    ): DrawerItem = when (this) {
        is Filter -> DrawerItem.Filter(
            title = title,
            icon = resolveIcon(purchaseState),
            color = if (purchaseState.purchasedThemes() || ColorProvider.isFreeColor(tint)) tint else 0,
            adjustColor = tint != 0,
            count = count.takeIf { it != NO_COUNT } ?: try {
                taskDao.count(this)
            } catch (e: Exception) {
                Logger.e(e, tag = TAG) { "Failed to count tasks" }
                0
            },
            selected = selectedFilter?.let { areItemsTheSame(it) } ?: false,
            shareCount = if (this is CaldavFilter) principals else 0,
            filter = this,
        )
        is NavigationDrawerSubheader -> DrawerItem.Header(
            title = title ?: "",
            collapsed = isCollapsed,
            hasError = error,
            canAdd = addIntentRc != 0,
            hasChildren = childCount > 0,
            openTaskApp = openTaskApp,
            header = this,
        )
        is SignInPrompt -> DrawerItem.SignIn
        else -> throw IllegalArgumentException()
    }
}
