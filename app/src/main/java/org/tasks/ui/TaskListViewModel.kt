package org.tasks.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.AstridOrderingFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterImpl
import com.todoroo.astrid.api.SearchFilter
import com.todoroo.astrid.core.BuiltInFilterExposer
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.compose.throttleLatest
import org.tasks.data.DeletionDao
import org.tasks.data.TaskContainer
import org.tasks.data.TaskDao
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.db.QueryUtils
import org.tasks.filters.MyTasksFilter
import org.tasks.preferences.Preferences
import org.tasks.preferences.QueryPreferences
import org.tasks.tasklist.SectionedDataSource
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class TaskListViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val preferences: Preferences,
    private val taskDao: TaskDao,
    private val taskDeleter: TaskDeleter,
    private val deletionDao: DeletionDao,
    private val localBroadcastManager: LocalBroadcastManager,
    private val inventory: Inventory,
    private val firebase: Firebase,
) : ViewModel() {

    sealed class UiItem {
        data class Header(val value: Long): UiItem()
        data class Task(val task: TaskContainer): UiItem()
    }

    sealed interface TasksResults {
        data object Loading : TasksResults
        data class Results(val tasks: SectionedDataSource) : TasksResults
    }

    data class State(
        val filter: Filter = MyTasksFilter(""),
        val now: Long = DateUtilities.now(),
        val searchQuery: String? = null,
        val tasks: TasksResults = TasksResults.Loading,
        val begForSubscription: Boolean = false,
        val syncOngoing: Boolean = false,
        val collapsed: Set<Long> = setOf(SectionedDataSource.HEADER_COMPLETED),
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            invalidate()
        }
    }

    fun setFilter(filter: Filter) {
        _state.update {
            it.copy(filter = filter)
        }
    }

    fun setSearchQuery(query: String?) {
        _state.update { it.copy(searchQuery = query?.trim()) }
    }

    fun invalidate() {
        _state.update {
            it.copy(
                now = DateUtilities.now(),
                syncOngoing = preferences.isSyncOngoing,
            )
        }
    }

    fun dismissBanner(clickedPurchase: Boolean) {
        _state.update {
            it.copy(begForSubscription = false)
        }
        preferences.lastSubscribeRequest = DateUtilities.now()
        firebase.logEvent(R.string.event_banner_sub, R.string.param_click to clickedPurchase)
    }

    suspend fun getTasksToClear(): List<Long> {
        val filter = _state.value.filter
        val deleteFilter = FilterImpl(
            sql = QueryUtils.removeOrder(QueryUtils.showHiddenAndCompleted(filter.sql!!)),
        )
        val completed = taskDao.fetchTasks(
            object : QueryPreferences by preferences {
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

    init {
        localBroadcastManager.registerRefreshReceiver(refreshReceiver)

        _state
            .map { it.copy(tasks = TasksResults.Loading) }
            .distinctUntilChanged()
            .throttleLatest(333)
            .map {
                val filter = when {
                    it.searchQuery == null -> it.filter
                    it.searchQuery.isBlank() -> BuiltInFilterExposer.getMyTasksFilter(context.resources)
                    else -> context.createSearchQuery(it.searchQuery)
                }
                taskDao.fetchTasks { getQuery(preferences, filter) }
            }
            .onEach { tasks ->
                _state.update {
                    it.copy(
                        tasks = TasksResults.Results(
                            SectionedDataSource(
                                tasks = tasks,
                                disableHeaders = !it.filter.supportsSorting()
                                        || (it.filter.supportsManualSort() && preferences.isManualSort)
                                        || (it.filter is AstridOrderingFilter && preferences.isAstridSort),
                                groupMode = preferences.groupMode,
                                subtaskMode = preferences.subtaskMode,
                                collapsed = it.collapsed,
                                completedAtBottom = preferences.completedTasksAtBottom,
                            )
                        )
                    )
                }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)

        viewModelScope.launch(Dispatchers.Default) {
            if (!inventory.hasPro && !firebase.subscribeCooldown) {
                _state.update {
                    it.copy(begForSubscription = true)
                }
            }
        }
    }

    override fun onCleared() {
        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }

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

    companion object {
        fun Context.createSearchQuery(query: String): Filter =
            SearchFilter(getString(R.string.FLA_search_filter, query), query)
    }
}
