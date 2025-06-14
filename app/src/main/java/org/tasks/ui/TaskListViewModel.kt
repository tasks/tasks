package org.tasks.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import org.tasks.TasksApplication.Companion.IS_GENERIC
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.compose.throttleLatest
import org.tasks.data.TaskContainer
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.DeletionDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.data.fetchTasks
import org.tasks.db.QueryUtils
import org.tasks.extensions.Context.canScheduleExactAlarms
import org.tasks.filters.AstridOrderingFilter
import org.tasks.filters.EmptyFilter
import org.tasks.filters.Filter
import org.tasks.filters.FilterImpl
import org.tasks.filters.MyTasksFilter
import org.tasks.filters.SearchFilter
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.preferences.QueryPreferences
import org.tasks.tasklist.SectionedDataSource
import org.tasks.tasklist.TasksResults
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import javax.inject.Inject

sealed class Banner {
    data object NotificationsDisabled : Banner()
    data object AlarmsDisabled : Banner()
    data object QuietHoursEnabled : Banner()
    data object BegForMoney : Banner()
    data object WarnMicrosoft : Banner()
    data object WarnGoogleTasks : Banner()
    data object AppUpdated : Banner()
}

@HiltViewModel
class TaskListViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val preferences: Preferences,
    private val taskDao: TaskDao,
    private val taskDeleter: TaskDeleter,
    private val deletionDao: DeletionDao,
    private val localBroadcastManager: LocalBroadcastManager,
    private val inventory: Inventory,
    private val firebase: Firebase,
    private val permissionChecker: PermissionChecker,
    private val caldavDao: CaldavDao,
) : ViewModel() {

    data class State(
        val filter: Filter = EmptyFilter(),
        val now: Long = currentTimeMillis(),
        val searchQuery: String? = null,
        val tasks: TasksResults = TasksResults.Loading,
        val syncOngoing: Boolean = false,
        val collapsed: Set<Long> = setOf(SectionedDataSource.HEADER_COMPLETED),
        val banner: Banner? = null
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
                now = currentTimeMillis(),
                syncOngoing = preferences.isSyncOngoing,
            )
        }
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
                    it.searchQuery.isBlank() -> MyTasksFilter.create()
                    else -> applicationContext.createSearchQuery(it.searchQuery)
                }
                taskDao.fetchTasks(getQuery(preferences, filter))
            }
            .onEach { tasks ->
                _state.update {
                    it.copy(
                        tasks = TasksResults.Results(
                            SectionedDataSource(
                                tasks = tasks,
                                disableHeaders = it.filter.disableHeaders()
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

    fun updateBannerState() {
        viewModelScope.launch(Dispatchers.IO) {
            val accounts = caldavDao.getAccounts()
            val banner = when {
                preferences.getBoolean(R.string.p_just_updated, false) ->
                    Banner.AppUpdated
                preferences.warnNotificationsDisabled && !permissionChecker.hasNotificationPermission() ->
                    Banner.NotificationsDisabled
                preferences.warnAlarmsDisabled && !applicationContext.canScheduleExactAlarms() ->
                    Banner.AlarmsDisabled
                (IS_GENERIC || !inventory.hasPro) && !firebase.subscribeCooldown ->
                    Banner.BegForMoney
                preferences.isCurrentlyQuietHours && preferences.warnQuietHoursDisabled ->
                    Banner.QuietHoursEnabled
                accounts.any { it.isMicrosoft } && preferences.warnMicrosoft ->
                    Banner.WarnMicrosoft
                accounts.any { it.isGoogleTasks } && preferences.warnGoogleTasks ->
                    Banner.WarnGoogleTasks
                else -> null
            }
            _state.update {
                it.copy(banner = banner)
            }
        }
    }

    fun dismissBanner(tookAction: Boolean = false) {
        when (state.value.banner) {
            Banner.NotificationsDisabled -> preferences.warnNotificationsDisabled = tookAction
            Banner.AlarmsDisabled -> preferences.warnAlarmsDisabled = tookAction
            Banner.QuietHoursEnabled -> preferences.warnQuietHoursDisabled = false
            Banner.BegForMoney -> {
                preferences.lastSubscribeRequest = currentTimeMillis()
                firebase.logEvent(R.string.event_banner_sub, R.string.param_click to tookAction)
            }
            Banner.WarnGoogleTasks -> preferences.warnGoogleTasks = false
            Banner.WarnMicrosoft -> preferences.warnMicrosoft = false
            Banner.AppUpdated -> preferences.setBoolean(R.string.p_just_updated, false)
            null -> {}
        }

        updateBannerState()
    }

    companion object {
        fun Context.createSearchQuery(query: String): Filter =
            SearchFilter(getString(R.string.FLA_search_filter, query), query)
    }
}
