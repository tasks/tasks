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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.billing.PurchaseState
import org.tasks.caldav.CaldavClientProvider
import org.tasks.compose.settings.CaldavCalendarSettingsState
import org.tasks.compose.settings.buildPickerColors
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
import org.tasks.themes.TasksIcons
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
    private val purchaseState: PurchaseState,
    private val isDark: Boolean,
    private val hasColorWheel: Boolean = false,
) : ViewModel() {

    private val pickerColors = buildPickerColors(isDark)

    private val _state = MutableStateFlow(
        CaldavCalendarSettingsState(
            pickerColors = pickerColors,
            hasPro = purchaseState.purchasedThemes(),
            hasColorWheel = hasColorWheel,
        )
    )
    val state: StateFlow<CaldavCalendarSettingsState> = _state

    private val calendarId = MutableStateFlow<Long?>(null)

    val principals: StateFlow<List<PrincipalWithAccess>> = calendarId
        .flatMapLatest { id ->
            id?.let { principalDao.getPrincipals(it) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            principals.collect { list ->
                _state.update { it.copy(principals = list) }
            }
        }
    }

    fun setCalendar(account: CaldavAccount, calendar: CaldavCalendar?) {
        if (_state.value.account != null) return
        _state.value = CaldavCalendarSettingsState(
            name = calendar?.name ?: "",
            color = calendar?.color ?: 0,
            icon = calendar?.icon ?: TasksIcons.LIST,
            calendar = calendar,
            account = account,
            pickerColors = pickerColors,
            hasPro = purchaseState.purchasedThemes(),
            hasColorWheel = hasColorWheel,
        )
        calendarId.value = calendar?.id
    }

    open fun setName(value: String) {
        _state.update { it.copy(name = value, nameError = null) }
    }

    open fun setColor(value: Int) {
        _state.update { it.copy(color = value) }
    }

    open fun setIcon(value: String) {
        _state.update { it.copy(icon = value) }
    }

    fun openColorPicker() {
        _state.update { it.copy(showColorPicker = true, hasPro = purchaseState.purchasedThemes()) }
    }

    fun closeColorPicker() {
        _state.update { it.copy(showColorPicker = false) }
    }

    fun selectColor(color: Int) {
        _state.update { it.copy(color = color, showColorPicker = false) }
    }

    fun openIconPicker() {
        _state.update { it.copy(showIconPicker = true, hasPro = purchaseState.purchasedThemes()) }
    }

    fun closeIconPicker() {
        _state.update { it.copy(showIconPicker = false) }
    }

    fun selectIcon(name: String) {
        _state.update { it.copy(icon = name, showIconPicker = false) }
    }

    fun dismissSnackbar() {
        _state.update { it.copy(snackbar = null) }
    }

    fun showDiscardDialog() {
        _state.update { it.copy(showDiscardDialog = true) }
    }

    fun dismissDiscardDialog() {
        _state.update { it.copy(showDiscardDialog = false) }
    }

    fun openShareDialog() {
        _state.update { it.copy(shareDialogOpen = true) }
    }

    fun closeShareDialog() {
        _state.update { it.copy(shareDialogOpen = false) }
    }

    fun confirmRemovePrincipal(principal: PrincipalWithAccess?) {
        _state.update { it.copy(confirmRemovePrincipal = principal) }
    }

    fun removePrincipal(principal: PrincipalWithAccess) {
        val s = _state.value
        val account = s.account ?: return
        val calendar = s.calendar ?: return
        viewModelScope.launch {
            _state.update { it.copy(confirmRemovePrincipal = null, isLoading = true) }
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
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun share(input: String) {
        val s = _state.value
        val account = s.account ?: return
        viewModelScope.launch {
            _state.update { it.copy(shareLoading = true) }
            try {
                withContext(NonCancellable) {
                    if (s.isNew) {
                        createCalendar(skipFinish = true) ?: return@withContext
                    }
                    val calendar = _state.value.calendar ?: return@withContext
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
                    _state.update { it.copy(shareDialogOpen = false) }
                }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _state.update { it.copy(shareLoading = false) }
            }
        }
    }

    fun save(onComplete: (CaldavCalendar) -> Unit) {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            val s = _state.value
            val name = s.name.trim()
            if (name.isEmpty()) {
                _state.update { it.copy(nameError = getString(Res.string.name_cannot_be_empty)) }
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
        val s = _state.value
        val account = s.account ?: return null
        val name = s.name.trim()
        _state.update { it.copy(isLoading = true) }
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
                _state.update { it.copy(calendar = inserted) }
                calendarId.value = inserted?.id
                inserted
            }
        } catch (e: Exception) {
            handleError(e)
            null
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun updateCalendar(): CaldavCalendar? {
        val s = _state.value
        val account = s.account ?: return null
        val calendar = s.calendar ?: return null
        val name = s.name.trim()
        _state.update { it.copy(isLoading = true) }
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
                _state.update { it.copy(calendar = result) }
                result
            }
        } catch (e: Exception) {
            handleError(e)
            null
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun delete(onComplete: () -> Unit) {
        val s = _state.value
        val account = s.account ?: return
        val calendar = s.calendar ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
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
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun handleError(e: Exception) {
        Logger.e(e) { "CalDAV calendar operation failed" }
        _state.update {
            it.copy(
                snackbar = when (e) {
                    is HttpException -> {
                        e.message
                    }
                    is DisplayableException -> getString(e.resource)
                    is ConnectException -> getString(Res.string.network_error)
                    else -> getString(Res.string.error_adding_account, e.message ?: "")
                }
            )
        }
    }

}
