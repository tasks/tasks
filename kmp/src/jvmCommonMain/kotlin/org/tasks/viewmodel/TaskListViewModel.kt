package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.launch
import org.tasks.compose.throttleLatest
import org.tasks.data.TaskContainer
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.data.TaskSaver
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
import org.tasks.kmp.org.tasks.time.DateFormatter
import org.tasks.preferences.DefaultQueryPreferences
import org.tasks.preferences.FilterPreferences
import org.tasks.preferences.QueryPreferences
import org.tasks.preferences.TasksPreferences
import org.tasks.service.TaskCompleter
import org.tasks.service.TaskDeleter
import org.tasks.sync.SyncSource
import org.tasks.tasklist.HeaderFormatter
import org.tasks.tasklist.SectionedDataSource
import org.tasks.tasklist.TasksResults
import org.tasks.time.DateTimeUtils2.currentTimeMillis

open class TaskListViewModel(
    private val taskDao: TaskDao,
    private val taskDeleter: TaskDeleter,
    private val deletionDao: DeletionDao,
    private val taskSaver: TaskSaver,
    private val taskCompleter: TaskCompleter,
    private val tasksPreferences: TasksPreferences,
    private val headerFormatter: HeaderFormatter,
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
                override var showCompleted: Boolean
                    get() = true
                    set(_) {}
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

    fun onCompleteTask(taskContainer: TaskContainer, newState: Boolean) {
        viewModelScope.launch {
            taskCompleter.setComplete(taskContainer.task, newState)
        }
    }

    fun toggleSubtasks(taskId: Long, collapsed: Boolean) {
        viewModelScope.launch {
            taskSaver.setCollapsed(taskId, collapsed)
        }
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
            .map { queriedState ->
                val filter = when {
                    queriedState.searchQuery == null -> queriedState.filter
                    queriedState.searchQuery.isBlank() -> MyTasksFilter(title = "My Tasks")
                    else -> createSearchFilter(queriedState.searchQuery)
                }
                val prefs = if (isPerListSortEnabled) {
                    FilterPreferences(queryPreferences, tasksPreferences, filter.key())
                } else {
                    queryPreferences
                }
                Triple(taskDao.fetchTasks(getQuery(prefs, filter)), prefs, queriedState)
            }
            .onEach { (tasks, prefs, queriedState) ->
                val dataSource = SectionedDataSource(
                    tasks = tasks,
                    disableHeaders = queriedState.filter.disableHeaders()
                            || (queriedState.filter.supportsManualSort() && prefs.isManualSort)
                            || (queriedState.filter is AstridOrderingFilter && prefs.isAstridSort),
                    groupMode = prefs.groupMode,
                    subtaskMode = prefs.subtaskMode,
                    collapsed = queriedState.collapsed,
                    completedAtBottom = prefs.completedTasksAtBottom,
                )
                if (queriedState.filter.supportsSorting()) {
                    val dateFormatter = DateFormatter.create(is24HourFormat = false)
                    dataSource.formatHeaders { value ->
                        try {
                            headerFormatter.headerString(value, prefs.groupMode, dateFormatter)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            log.e(e) {
                                "Failed to format header $value (groupMode=${prefs.groupMode})"
                            }
                            null
                        }
                    }
                }
                _state.update {
                    if (it.filter == queriedState.filter &&
                        it.searchQuery == queriedState.searchQuery &&
                        it.collapsed == queriedState.collapsed
                    ) {
                        it.copy(tasks = TasksResults.Results(dataSource))
                    } else {
                        it
                    }
                }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    companion object {
        private val log = Logger.withTag("TaskListViewModel")
    }
}
