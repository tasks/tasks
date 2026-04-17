package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.jsonPrimitive
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.caldav.TasksAccountResponse
import org.tasks.compose.settings.NewPassword
import org.tasks.compose.settings.SharedCalendarDisplay
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.PrincipalDao
import org.tasks.data.entity.CaldavAccount

open class TasksAccountViewModel(
    private val provider: CaldavClientProvider,
    private val reporting: Reporting,
    private val accountDataRepository: TasksAccountDataRepository,
    private val caldavDao: CaldavDao,
    private val principalDao: PrincipalDao,
) : ViewModel() {
    val newPassword = MutableStateFlow<NewPassword?>(null)
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

    val appPasswords: StateFlow<List<TasksAccountResponse.AppPassword>?> = accountResponse
        .map { it?.appPasswords }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val sharedWithMe: StateFlow<List<SharedCalendarDisplay>> =
        accountResponse
            .combine(caldavDao.subscribeToCalendars()) { response, calendars ->
                response to calendars
            }
            .combine(accountUuid.filterNotNull()) { (response, calendars), uuid ->
                Triple(response, calendars.filter { it.account == uuid }, uuid)
            }
            .mapLatest { (response, calendars, _) ->
                val uris = response?.sharedWithMe ?: emptyList()
                uris.mapNotNull { uri ->
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
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val inboundEmail: StateFlow<String?> = accountResponse
        .map { it?.inboundEmail?.email?.takeIf(String::isNotEmpty) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val inboundCalendar: StateFlow<String?> = accountResponse
        .map { it?.inboundEmail?.calendar?.takeIf(String::isNotEmpty) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isGuest: StateFlow<Boolean> = accountResponse
        .map { it?.guest ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val guests: StateFlow<List<TasksAccountResponse.Guest>> = accountResponse
        .map { it?.guests ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val maxGuests: StateFlow<Int> = accountResponse
        .map { it?.maxGuests ?: 5 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 5)

    private val passwordMutex = Mutex()

    private suspend fun refreshAccountData(account: CaldavAccount) {
        try {
            accountDataRepository.fetchAndCache(account)
        } catch (e: Exception) {
            Logger.e(e) { "Failed to refresh account data" }
        }
    }

    fun refreshAccount(account: CaldavAccount) = viewModelScope.launch {
        refreshAccountData(account)
    }

    fun requestNewPassword(account: CaldavAccount, description: String) = viewModelScope.launch {
        if (!passwordMutex.tryLock()) {
            return@launch
        }
        try {
            provider
                    .forTasksAccount(account)
                    .generateNewPassword(description.takeIf { it.isNotBlank() })
                    ?.let {
                        newPassword.value =
                                NewPassword(
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

    fun deletePassword(account: CaldavAccount, id: Int) = viewModelScope.launch {
        try {
            provider.forTasksAccount(account).deletePassword(id)
            refreshAccountData(account)
        } catch (e: Exception) {
            Logger.e(e) { "Failed to delete password" }
        }
    }

    fun clearNewPassword() {
        newPassword.value = null
    }

    fun regenerateInboundEmail(account: CaldavAccount) = viewModelScope.launch {
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

    fun setInboundCalendar(account: CaldavAccount, calendar: String?) = viewModelScope.launch {
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
}
