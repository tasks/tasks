package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.jsonPrimitive
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.billing.SubscriptionProvider
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.caldav.TasksAccountResponse
import org.tasks.compose.settings.CalendarItem
import org.tasks.compose.settings.NewPassword
import org.tasks.compose.settings.SharedCalendarDisplay
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.PrincipalDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.fcm.PushTokenManager
import org.tasks.jobs.BackgroundWork
import org.tasks.preferences.TasksPreferences
import org.tasks.service.TaskDeleter
import org.tasks.sync.SyncSource

data class TasksAccountState(
    val account: CaldavAccount? = null,
    val isGithub: Boolean = false,
    val isGuest: Boolean = false,
    val hasSubscription: Boolean = false,
    val isTasksSubscription: Boolean = false,
    val localListCount: Int = 0,
    val showTosDialog: Boolean = false,
    val inboundEmail: String? = null,
    val inboundCalendarName: String? = null,
    val appPasswords: List<TasksAccountResponse.AppPassword>? = null,
    val sharedWithMe: List<SharedCalendarDisplay> = emptyList(),
    val guests: List<TasksAccountResponse.Guest> = emptyList(),
    val maxGuests: Int = 5,
    val newPassword: NewPassword? = null,
    val calendars: List<CalendarItem> = emptyList(),
    val inboundCalendarUri: String? = null,
    val caldavUrl: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
open class TasksAccountViewModel(
    private val provider: CaldavClientProvider,
    private val reporting: Reporting,
    private val accountDataRepository: TasksAccountDataRepository,
    private val caldavDao: CaldavDao,
    private val principalDao: PrincipalDao,
    private val backgroundWork: BackgroundWork,
    private val pushTokenManager: PushTokenManager,
    private val taskDeleter: TaskDeleter,
    private val tasksPreferences: TasksPreferences,
    subscriptionProvider: SubscriptionProvider,
    caldavUrl: String,
) : ViewModel() {
    private val _newPassword = MutableStateFlow<NewPassword?>(null)
    private val _tosDismissed = MutableStateFlow(false)
    private val accountUuid = MutableStateFlow<String?>(null)

    fun setAccountUuid(uuid: String) {
        accountUuid.value = uuid
    }

    private val initialResponse: TasksAccountResponse? = runBlocking {
        accountDataRepository.getAccountResponse()
    }

    private val accountResponse: StateFlow<TasksAccountResponse?> =
        accountDataRepository.accountResponseFlow
            .stateIn(viewModelScope, SharingStarted.Eagerly, initialResponse)

    private val account: StateFlow<CaldavAccount?> = accountUuid
        .filterNotNull()
        .flatMapLatest { uuid ->
            caldavDao.watchAccounts().map { accounts ->
                accounts.firstOrNull { it.uuid == uuid }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val allCalendars = caldavDao.subscribeToCalendars()

    private val accountCalendars = allCalendars
        .combine(accountUuid.filterNotNull()) { calendars, uuid ->
            calendars.filter { it.account == uuid }
        }

    private val inboundCalendarUri = accountResponse
        .map { it?.inboundEmail?.calendar?.takeIf(String::isNotEmpty) }

    private val localListCount = accountUuid
        .filterNotNull()
        .flatMapLatest {
            caldavDao.watchAccounts().map { accounts ->
                val localAccount = accounts.firstOrNull { it.accountType == TYPE_LOCAL }
                localAccount?.uuid?.let { uuid -> caldavDao.listCount(uuid) } ?: 0
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val state: StateFlow<TasksAccountState> = combine(
        account,
        accountResponse,
        subscriptionProvider.subscription,
        accountCalendars,
        inboundCalendarUri,
        localListCount,
        _newPassword,
        _tosDismissed,
    ) { values ->
        val account = values[0] as CaldavAccount?
        val response = values[1] as TasksAccountResponse?
        val subscription = values[2] as SubscriptionProvider.SubscriptionInfo?
        @Suppress("UNCHECKED_CAST")
        val calendars = values[3] as List<org.tasks.data.entity.CaldavCalendar>
        val inboundUri = values[4] as String?
        val localCount = values[5] as Int
        val newPassword = values[6] as NewPassword?
        val tosDismissed = values[7] as Boolean

        TasksAccountState(
            account = account,
            isGithub = account?.username?.startsWith("github") == true,
            isGuest = response?.guest ?: false,
            hasSubscription = subscription != null,
            isTasksSubscription = subscription?.isTasksSubscription == true,
            localListCount = localCount,
            showTosDialog = !tosDismissed && account?.isTosRequired() == true,
            inboundEmail = response?.inboundEmail?.email?.takeIf(String::isNotEmpty),
            inboundCalendarName = calendars
                .find { it.calendarUri == inboundUri }?.name,
            appPasswords = response?.appPasswords,
            sharedWithMe = buildSharedWithMe(response, calendars),
            guests = response?.guests ?: emptyList(),
            maxGuests = response?.maxGuests ?: 5,
            newPassword = newPassword,
            calendars = calendars
                .filter { !it.readOnly() }
                .map { CalendarItem(it.name ?: it.uuid ?: "", it.calendarUri) },
            inboundCalendarUri = inboundUri,
            caldavUrl = caldavUrl,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TasksAccountState(caldavUrl = caldavUrl))

    private suspend fun buildSharedWithMe(
        response: TasksAccountResponse?,
        calendars: List<org.tasks.data.entity.CaldavCalendar>,
    ): List<SharedCalendarDisplay> {
        val uris = response?.sharedWithMe ?: return emptyList()
        return uris.mapNotNull { uri ->
            val cal = calendars.find { it.calendarUri == uri }
                ?: return@mapNotNull null
            val ownerName = principalDao.getOwnerName(cal.id)
            SharedCalendarDisplay(
                name = cal.name ?: uri,
                icon = cal.icon,
                color = cal.color,
                ownerName = ownerName,
            )
        }
    }

    private val passwordMutex = Mutex()

    private suspend fun refreshAccountData(account: CaldavAccount) {
        try {
            accountDataRepository.fetchAndCache(account)
        } catch (e: Exception) {
            Logger.e(e) { "Failed to refresh account data" }
        }
    }

    fun refreshAccount() {
        viewModelScope.launch {
            account.value?.let { refreshAccountData(it) }
        }
    }

    fun requestNewPassword(description: String) = viewModelScope.launch {
        val account = account.value ?: return@launch
        if (!passwordMutex.tryLock()) return@launch
        try {
            provider
                .forTasksAccount(account)
                .generateNewPassword(description.takeIf { it.isNotBlank() })
                ?.let {
                    _newPassword.value = NewPassword(
                        username = it["username"]!!.jsonPrimitive.content,
                        password = it["password"]!!.jsonPrimitive.content,
                    )
                }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to request new password" }
        } finally {
            passwordMutex.unlock()
        }
    }

    fun deletePassword(id: Int) = viewModelScope.launch {
        val account = account.value ?: return@launch
        try {
            provider.forTasksAccount(account).deletePassword(id)
            refreshAccountData(account)
        } catch (e: Exception) {
            Logger.e(e) { "Failed to delete password" }
        }
    }

    fun clearNewPassword() {
        _newPassword.value = null
    }

    fun acceptTos(tosVersion: Int) {
        _tosDismissed.value = true
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.acceptedTosVersion, tosVersion)
            account.value?.let {
                caldavDao.update(it.copy(error = null))
            }
            backgroundWork.sync(SyncSource.ACCOUNT_ADDED)
        }
    }

    fun dismissTos() {
        _tosDismissed.value = true
    }

    fun regenerateInboundEmail() = viewModelScope.launch {
        val account = account.value ?: return@launch
        try {
            provider.forTasksAccount(account).regenerateInboundEmail()
            refreshAccountData(account)
            reporting.logEvent(
                AnalyticsEvents.SETTINGS_CLICK,
                AnalyticsEvents.PARAM_TYPE to "email_to_task_regenerate"
            )
        } catch (e: Exception) {
            Logger.e(e) { "Failed to regenerate inbound email" }
        }
    }

    fun migrateLocalTasks() {
        account.value?.let { backgroundWork.migrateLocalTasks(it) }
    }

    suspend fun logout(account: CaldavAccount) {
        pushTokenManager.unregisterToken(account)
        taskDeleter.delete(account)
        accountDataRepository.clear()
    }

    fun setInboundCalendar(calendar: String?) = viewModelScope.launch {
        val account = account.value ?: return@launch
        try {
            provider.forTasksAccount(account).setInboundCalendar(calendar)
            refreshAccountData(account)
            reporting.logEvent(
                AnalyticsEvents.SETTINGS_CLICK,
                AnalyticsEvents.PARAM_TYPE to "email_to_task_set_calendar"
            )
        } catch (e: Exception) {
            Logger.e(e) { "Failed to set inbound calendar" }
        }
    }

    companion object {
        const val DEFAULT_TOS_VERSION = 1
    }
}
