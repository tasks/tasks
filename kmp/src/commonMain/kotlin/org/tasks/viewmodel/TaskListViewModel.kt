package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.tasks.compose.throttleLatest
import org.tasks.data.TaskContainer
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.data.dao.DeletionDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.data.fetchTasks
import org.tasks.db.QueryUtils
import org.tasks.filters.AstridOrderingFilter
import org.tasks.filters.EmptyFilter
import org.tasks.filters.Filter
import org.tasks.filters.FilterImpl
import org.tasks.filters.MyTasksFilter
import org.tasks.filters.SearchFilter
import org.tasks.filters.key
import org.tasks.preferences.DefaultQueryPreferences
import org.tasks.preferences.FilterPreferences
import org.tasks.preferences.QueryPreferences
import org.tasks.preferences.TasksPreferences
import org.tasks.service.TaskDeleter
import org.tasks.sync.SyncSource
import org.tasks.tasklist.SectionedDataSource
import org.tasks.tasklist.TasksResults
import org.tasks.time.DateTimeUtils2.currentTimeMillis

open class TaskListViewModel(
    private val taskDao: TaskDao,
    private val taskDeleter: TaskDeleter,
    private val deletionDao: DeletionDao,
    private val tasksPreferences: TasksPreferences,
    private val queryPreferences: QueryPreferences = DefaultQueryPreferences(),
    private val isPerListSortEnabled: Boolean = false,
    private val createSearchFilter: (String) -> Filter = { query ->
        SearchFilter(title = query, query = query)
    },
    refreshFlow: Flow<Unit> = emptyFlow(),
) : ViewModel() {

    data class State(
        val filter: Filter = EmptyFilter(),
        val now: Long = currentTimeMillis(),
        val searchQuery: String? = null,
        val tasks: TasksResults = TasksResults.Loading,
        val syncOngoing: Boolean = false,
        val collapsed: Set<Long> = setOf(SectionedDataSource.HEADER_COMPLETED),
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    fun setFilter(filter: Filter) {
        _state.update { it.copy(filter = filter) }
    }

    fun setSearchQuery(query: String?) {
        _state.update { it.copy(searchQuery = query?.trim()) }
    }

    fun invalidate() {
        _state.update { it.copy(now = currentTimeMillis()) }
    }

    suspend fun getTasksToClear(): List<Long> {
        val filter = _state.value.filter
        val deleteFilter = FilterImpl(
            sql = QueryUtils.removeOrder(QueryUtils.showHiddenAndCompleted(filter.sql!!)),
        )
        val completed = taskDao.fetchTasks(
            object : QueryPreferences by queryPreferences {
                override val showCompleted: Boolean
                    get() = true
            },
            deleteFilter
        )
            .filter(TaskContainer::isCompleted)
            .filterNot(TaskContainer::isReadOnly)
            .map(TaskContainer::id)
            .toMutableList()
        completed.removeAll(deletionDao.hasRecurringAncestors(completed))
        return completed
    }

    suspend fun markDeleted(tasks: List<Long>): List<Task> =
        taskDeleter.markDeleted(tasks)

    fun clearCollapsed() {
        _state.update {
            it.copy(collapsed = setOf(SectionedDataSource.HEADER_COMPLETED))
        }
    }

    fun toggleCollapsed(group: Long) {
        _state.update {
            it.copy(
                collapsed = if (it.collapsed.contains(group)) {
                    it.collapsed.minus(group)
                } else {
                    it.collapsed.plus(group)
                }
            )
        }
    }

    init {
        refreshFlow
            .onEach { invalidate() }
            .launchIn(viewModelScope)

        tasksPreferences
            .flow(TasksPreferences.syncSource, SyncSource.NONE.name)
            .map { SyncSource.fromString(it).showIndicator }
            .distinctUntilChanged()
            .onEach { syncOngoing -> _state.update { it.copy(syncOngoing = syncOngoing) } }
            .launchIn(viewModelScope)

        _state
            .map { it.copy(tasks = TasksResults.Loading) }
            .distinctUntilChanged()
            .throttleLatest(333)
            .map {
                val filter = when {
                    it.searchQuery == null -> it.filter
                    it.searchQuery.isBlank() -> MyTasksFilter(title = "My Tasks")
                    else -> createSearchFilter(it.searchQuery)
                }
                val prefs = if (isPerListSortEnabled) {
                    FilterPreferences(queryPreferences, tasksPreferences, filter.key())
                } else {
                    queryPreferences
                }
                Pair(taskDao.fetchTasks(getQuery(prefs, filter)), prefs)
            }
            .onEach { (tasks, prefs) ->
                _state.update {
                    it.copy(
                        tasks = TasksResults.Results(
                            SectionedDataSource(
                                tasks = tasks,
                                disableHeaders = it.filter.disableHeaders()
                                        || (it.filter.supportsManualSort() && prefs.isManualSort)
                                        || (it.filter is AstridOrderingFilter && prefs.isAstridSort),
                                groupMode = prefs.groupMode,
                                subtaskMode = prefs.subtaskMode,
                                collapsed = it.collapsed,
                                completedAtBottom = prefs.completedTasksAtBottom,
                            )
                        )
                    )
                }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }
}
