package org.tasks.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.broadcast.ComposeRefreshBroadcaster
import org.tasks.data.TaskSaver
import org.tasks.service.TaskCompleter
import org.tasks.TasksApplication.Companion.IS_GENERIC
import org.tasks.TasksApplication.Companion.IS_GOOGLE_PLAY
import androidx.annotation.StringRes
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.DeletionDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.prefTitle
import org.tasks.extensions.Context.canScheduleExactAlarms
import org.tasks.filters.SearchFilter
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.preferences.TasksPreferences
import org.tasks.service.TaskDeleter
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
    taskDao: TaskDao,
    taskDeleter: TaskDeleter,
    deletionDao: DeletionDao,
    taskSaver: TaskSaver,
    taskCompleter: TaskCompleter,
    private val tasksPreferences: TasksPreferences,
    composeRefreshBroadcaster: ComposeRefreshBroadcaster,
    private val inventory: Inventory,
    private val firebase: Firebase,
    private val permissionChecker: PermissionChecker,
    private val caldavDao: CaldavDao,
) : org.tasks.viewmodel.TaskListViewModel(
    taskDao = taskDao,
    taskDeleter = taskDeleter,
    deletionDao = deletionDao,
    taskSaver = taskSaver,
    taskCompleter = taskCompleter,
    tasksPreferences = tasksPreferences,
    queryPreferences = preferences,
    isPerListSortEnabled = preferences.isPerListSortEnabled,
    createSearchFilter = { query ->
        SearchFilter(applicationContext.getString(R.string.FLA_search_filter, query), query)
    },
    refreshFlow = composeRefreshBroadcaster.refreshes,
) {
    private val _banner = MutableStateFlow<Banner?>(null)
    val banner = _banner.asStateFlow()

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
            if (banner != null && banner != _banner.value) {
                firebase.logEvent(
                    R.string.event_banner,
                    R.string.param_type to banner.eventType,
                    R.string.param_action to "shown",
                )
            }
            _banner.value = banner
        }
    }

    fun dismissBanner(tookAction: Boolean = false) {
        val currentBanner = _banner.value ?: return

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
}
