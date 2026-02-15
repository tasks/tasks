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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.TasksApplication.Companion.IS_GENERIC
import org.tasks.TasksApplication.Companion.IS_GOOGLE_PLAY
import androidx.annotation.StringRes
import org.tasks.data.prefTitle
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.compose.throttleLatest
import org.tasks.data.TaskContainer
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
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
import org.tasks.preferences.TasksPreferences
import org.tasks.sync.SyncSource
import org.tasks.tasklist.SectionedDataSource
import org.tasks.tasklist.TasksResults
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import javax.inject.Inject

sealed class Banner(
    val eventType: String,
    @StringRes val positiveLabel: Int,
    @StringRes val negativeLabel: Int,
) {
    data object NotificationsDisabled : Banner("notifications", R.string.TLA_menu_settings, R.string.dismiss)
    data object AlarmsDisabled : Banner("alarms", R.string.TLA_menu_settings, R.string.dismiss)
    data class SubscriptionRequired(val nameRes: Int, val isTasksOrg: Boolean) : Banner("subscribe", R.string.button_subscribe, R.string.dismiss)
    data object BegForMoney : Banner(
        "subscribe",
        if (IS_GENERIC) R.string.donate_today else R.string.button_subscribe,
        R.string.donate_maybe_later,
    )
    data object WarnMicrosoft : Banner("microsoft", R.string.button_learn_more, R.string.dismiss)
    data object WarnGoogleTasks : Banner("google_tasks", R.string.button_learn_more, R.string.dismiss)
    data object AppUpdated : Banner("app_updated", R.string.whats_new, R.string.dismiss)
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
    private val tasksPreferences: TasksPreferences,
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
            it.copy(now = currentTimeMillis())
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
            val dismissedSubscriptionAccounts = tasksPreferences.get(
                TasksPreferences.subscriptionDismissedAccounts, emptySet()
            )
            val resolvedAccounts = accounts
                .filter { it.uuid in dismissedSubscriptionAccounts && !it.isSubscriptionRequired() }
                .mapNotNull { it.uuid }
                .toSet()
            if (resolvedAccounts.isNotEmpty()) {
                tasksPreferences.set(
                    TasksPreferences.subscriptionDismissedAccounts,
                    dismissedSubscriptionAccounts - resolvedAccounts
                )
            }
            val subscriptionAccounts = if (IS_GOOGLE_PLAY) {
                accounts.filter {
                    it.isSubscriptionRequired() && it.uuid !in dismissedSubscriptionAccounts
                }
            } else {
                emptyList()
            }
            val banner = when {
                subscriptionAccounts.isNotEmpty() -> {
                    val account = subscriptionAccounts.find { it.isTasksOrg }
                        ?: subscriptionAccounts.first()
                    Banner.SubscriptionRequired(
                        nameRes = account.prefTitle,
                        isTasksOrg = account.isTasksOrg,
                    )
                }
                preferences.getBoolean(R.string.p_just_updated, false) ->
                    Banner.AppUpdated
                preferences.warnNotificationsDisabled && !permissionChecker.hasNotificationPermission() ->
                    Banner.NotificationsDisabled
                preferences.warnAlarmsDisabled && !applicationContext.canScheduleExactAlarms() ->
                    Banner.AlarmsDisabled
                inventory.begForMoney && !firebase.subscribeCooldown ->
                    Banner.BegForMoney
                accounts.any { it.isMicrosoft } && preferences.warnMicrosoft ->
                    Banner.WarnMicrosoft
                accounts.any { it.isGoogleTasks } && preferences.warnGoogleTasks ->
                    Banner.WarnGoogleTasks
                else -> null
            }
            if (banner != null && banner != state.value.banner) {
                firebase.logEvent(
                    R.string.event_banner,
                    R.string.param_type to banner.eventType,
                    R.string.param_action to "shown",
                )
            }
            _state.update {
                it.copy(banner = banner)
            }
        }
    }

    fun dismissBanner(tookAction: Boolean = false) {
        val currentBanner = state.value.banner ?: return
        _state.update { it.copy(banner = null) }

        viewModelScope.launch(NonCancellable) {
            when (currentBanner) {
                Banner.NotificationsDisabled -> preferences.warnNotificationsDisabled = tookAction
                Banner.AlarmsDisabled -> preferences.warnAlarmsDisabled = tookAction
                is Banner.SubscriptionRequired -> {
                    withContext(Dispatchers.IO) {
                        val dismissed = tasksPreferences.get(
                            TasksPreferences.subscriptionDismissedAccounts, emptySet()
                        )
                        val uuids = caldavDao.getAccounts()
                            .filter { it.isSubscriptionRequired() }
                            .mapNotNull { it.uuid }
                        tasksPreferences.set(
                            TasksPreferences.subscriptionDismissedAccounts,
                            dismissed + uuids
                        )
                    }
                }
                Banner.BegForMoney -> preferences.lastSubscribeRequest = currentTimeMillis()
                Banner.WarnGoogleTasks -> preferences.warnGoogleTasks = false
                Banner.WarnMicrosoft -> preferences.warnMicrosoft = false
                Banner.AppUpdated -> preferences.setBoolean(R.string.p_just_updated, false)
            }
            val action = if (tookAction) "positive" else "negative"
            val labelResId = if (tookAction) currentBanner.positiveLabel else currentBanner.negativeLabel
            firebase.logEvent(
                R.string.event_banner,
                R.string.param_type to currentBanner.eventType,
                R.string.param_action to action,
                R.string.param_label to applicationContext.resources.getResourceEntryName(labelResId),
            )
            updateBannerState()
        }
    }

    private fun CaldavAccount.isSubscriptionRequired() =
        (!inventory.hasPro && needsPro) || (isTasksOrg && isPaymentRequired())

    companion object {
        fun Context.createSearchQuery(query: String): Filter =
            SearchFilter(getString(R.string.FLA_search_filter, query), query)
    }
}
