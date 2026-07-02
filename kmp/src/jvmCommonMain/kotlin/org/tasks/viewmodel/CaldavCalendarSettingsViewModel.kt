package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.bitfire.dav4jvm.okhttp.exception.HttpException
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.billing.PurchaseState
import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.PrincipalWithAccess
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.PrincipalDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_NEXTCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OWNCLOUD
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_WRITE
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_NO_RESPONSE
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_UNKNOWN
import org.tasks.service.TaskDeleter
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import org.tasks.ui.DisplayableException
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.error_adding_account
import tasks.kmp.generated.resources.name_cannot_be_empty
import tasks.kmp.generated.resources.network_error
import java.net.ConnectException

@OptIn(ExperimentalCoroutinesApi::class)
open class CaldavCalendarSettingsViewModel(
    private val caldavDao: CaldavDao,
    private val caldavClientProvider: CaldavClientProvider,
    private val principalDao: PrincipalDao,
    private val taskDeleter: TaskDeleter,
    private val syncAdapters: SyncAdapters,
    private val reporting: Reporting,
    purchaseState: PurchaseState,
    isDark: Boolean,
    account: CaldavAccount,
    calendar: CaldavCalendar,
    hasColorWheel: Boolean = false,
    internal val stateManager: ListSettingsStateManager = ListSettingsStateManager(isDark, purchaseState, account, calendar, hasColorWheel),
) : ViewModel(), ListSettingsCallbacks by stateManager {

    private val calendarId = MutableStateFlow(calendar.id.takeIf { it != 0L })

    val principals: StateFlow<List<PrincipalWithAccess>> = calendarId
        .flatMapLatest { id ->
            id?.let { principalDao.getPrincipals(it) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            principals.collect { list ->
                stateManager.update { it.copy(principals = list) }
            }
        }
        stateManager.observeTaskCount(viewModelScope, caldavDao)
    }

    open override fun setName(value: String) = stateManager.setName(value)
    open override fun setColor(value: Int) = stateManager.setColor(value)
    open override fun setIcon(value: String) = stateManager.setIcon(value)

    override fun openShareDialog() {
        stateManager.update { it.copy(shareDialogOpen = true) }
    }

    override fun closeShareDialog() {
        stateManager.update { it.copy(shareDialogOpen = false) }
    }

    override fun confirmRemovePrincipal(principal: PrincipalWithAccess?) {
        stateManager.update { it.copy(confirmRemovePrincipal = principal) }
    }

    override fun removePrincipal(principal: PrincipalWithAccess) {
        val s = state.value
        val account = s.account ?: return
        val calendar = s.calendar ?: return
        viewModelScope.launch {
            stateManager.update { it.copy(confirmRemovePrincipal = null, isLoading = true) }
            try {
                withContext(NonCancellable) {
                    withContext(Dispatchers.IO) {
                        caldavClientProvider.forAccount(account).removePrincipal(account, calendar, principal.href)
                    }
                    principalDao.deleteAccessById(principal.id)
                    syncAdapters.sync(SyncSource.SHARING_CHANGE)
                }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                stateManager.update { it.copy(isLoading = false) }
            }
        }
    }

    override fun share(input: String) {
        val s = state.value
        val account = s.account ?: return
        viewModelScope.launch {
            stateManager.update { it.copy(shareLoading = true) }
            try {
                withContext(NonCancellable) {
                    if (s.isNew) {
                        createCalendar(skipFinish = true) ?: return@withContext
                    }
                    val calendar = state.value.calendar ?: return@withContext
                    val href = when (account.serverType) {
                        SERVER_OWNCLOUD, SERVER_NEXTCLOUD -> "principal:principals/users/$input"
                        else -> "mailto:$input"
                    }
                    withContext(Dispatchers.IO) {
                        caldavClientProvider.forAccount(account, calendar.url!!).share(account, href)
                    }
                    val principal = principalDao.getOrCreatePrincipal(account, href)
                    val invite = if (href.startsWith("mailto:")) INVITE_NO_RESPONSE else INVITE_UNKNOWN
                    principalDao.getOrCreateAccess(calendar, principal, invite, ACCESS_READ_WRITE)
                    syncAdapters.sync(SyncSource.SHARING_CHANGE)
                    stateManager.update { it.copy(shareDialogOpen = false) }
                }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                stateManager.update { it.copy(shareLoading = false) }
            }
        }
    }

    fun save(onComplete: (CaldavCalendar) -> Unit) {
        if (state.value.isLoading) return
        viewModelScope.launch {
            val s = state.value
            val name = s.name.trim()
            if (name.isEmpty()) {
                val error = getString(Res.string.name_cannot_be_empty)
                stateManager.update { it.copy(nameError = error) }
                return@launch
            }
            if (s.isNew) {
                createCalendar(skipFinish = false)?.let { onComplete(it) }
            } else if (s.hasChanges) {
                updateCalendar()?.let { onComplete(it) }
            } else {
                s.calendar?.let { onComplete(it) }
            }
        }
    }

    private suspend fun createCalendar(skipFinish: Boolean): CaldavCalendar? {
        val s = state.value
        val account = s.account ?: return null
        val name = s.name.trim()
        stateManager.update { it.copy(isLoading = true) }
        return try {
            withContext(NonCancellable) {
                val url = withContext(Dispatchers.IO) {
                    caldavClientProvider.forAccount(account).makeCollection(name, s.color, s.icon)
                }
                val calendar = CaldavCalendar(
                    uuid = UUIDHelper.newUUID(),
                    account = account.uuid,
                    url = url,
                    name = name,
                    color = s.color,
                    icon = s.icon,
                )
                caldavDao.insert(calendar)
                reporting.logEvent(AnalyticsEvents.CREATE_LIST)
                val inserted = caldavDao.getCalendarByUuid(calendar.uuid!!)
                stateManager.update { it.copy(calendar = inserted) }
                calendarId.value = inserted?.id
                inserted
            }
        } catch (e: Exception) {
            handleError(e)
            null
        } finally {
            stateManager.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun updateCalendar(): CaldavCalendar? {
        val s = state.value
        val account = s.account ?: return null
        val calendar = s.calendar ?: return null
        val name = s.name.trim()
        stateManager.update { it.copy(isLoading = true) }
        return try {
            withContext(NonCancellable) {
                withContext(Dispatchers.IO) {
                    caldavClientProvider.forAccount(account, calendar.url!!)
                        .updateCollection(name, s.color, s.icon)
                }
                val result = calendar.copy(
                    name = name,
                    color = s.color,
                    icon = s.icon,
                )
                caldavDao.update(result)
                stateManager.update { it.copy(calendar = result) }
                result
            }
        } catch (e: Exception) {
            handleError(e)
            null
        } finally {
            stateManager.update { it.copy(isLoading = false) }
        }
    }

    fun delete(onComplete: () -> Unit) {
        val s = state.value
        val account = s.account ?: return
        val calendar = s.calendar ?: return
        if (s.isLoading) return
        viewModelScope.launch {
            stateManager.update { it.copy(isLoading = true) }
            try {
                withContext(NonCancellable) {
                    withContext(Dispatchers.IO) {
                        caldavClientProvider.forAccount(account, calendar.url!!)
                            .deleteCollection()
                    }
                    reporting.logEvent(AnalyticsEvents.SETTINGS_CLICK, AnalyticsEvents.PARAM_TYPE to AnalyticsEvents.SettingsClick.DELETE_LIST)
                    taskDeleter.delete(calendar)
                }
                onComplete()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                stateManager.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun handleError(e: Exception) {
        Logger.e(e) { "CalDAV calendar operation failed" }
        val message = when (e) {
            is HttpException -> e.message
            is DisplayableException -> getString(e.resource)
            is ConnectException -> getString(Res.string.network_error)
            else -> getString(Res.string.error_adding_account, e.message ?: "")
        }
        stateManager.update { it.copy(snackbar = message) }
    }
}
