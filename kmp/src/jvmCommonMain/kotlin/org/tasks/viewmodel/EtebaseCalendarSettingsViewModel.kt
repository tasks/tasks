package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.billing.PurchaseState
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.etebase.EtebaseClientProvider
import org.tasks.service.TaskDeleter
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.error_adding_account
import tasks.kmp.generated.resources.name_cannot_be_empty
import tasks.kmp.generated.resources.network_error
import java.net.ConnectException

open class EtebaseCalendarSettingsViewModel(
    private val caldavDao: CaldavDao,
    private val clientProvider: EtebaseClientProvider,
    private val taskDeleter: TaskDeleter,
    private val reporting: Reporting,
    purchaseState: PurchaseState,
    isDark: Boolean,
    account: CaldavAccount,
    calendar: CaldavCalendar,
    hasColorWheel: Boolean = false,
    internal val stateManager: ListSettingsStateManager = ListSettingsStateManager(isDark, purchaseState, account, calendar, hasColorWheel),
) : ViewModel(), ListSettingsCallbacks by stateManager {

    open override fun setName(value: String) = stateManager.setName(value)
    open override fun setColor(value: Int) = stateManager.setColor(value)
    open override fun setIcon(value: String) = stateManager.setIcon(value)

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
                createCalendar()?.let { onComplete(it) }
            } else if (s.hasChanges) {
                updateCalendar()?.let { onComplete(it) }
            } else {
                s.calendar?.let { onComplete(it) }
            }
        }
    }

    private suspend fun createCalendar(): CaldavCalendar? {
        val s = state.value
        val account = s.account ?: return null
        val name = s.name.trim()
        stateManager.update { it.copy(isLoading = true) }
        return try {
            withContext(NonCancellable) {
                val url = withContext(Dispatchers.IO) {
                    clientProvider.forAccount(account).makeCollection(name, s.color)
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
                    clientProvider.forAccount(account).updateCollection(calendar, name, s.color)
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
                        clientProvider.forAccount(account).deleteCollection(calendar)
                    }
                    reporting.logEvent(
                        AnalyticsEvents.SETTINGS_CLICK,
                        AnalyticsEvents.PARAM_TYPE to AnalyticsEvents.SettingsClick.DELETE_LIST,
                    )
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
        Logger.e(e) { "Etebase calendar operation failed" }
        val message = when (e) {
            is ConnectException -> getString(Res.string.network_error)
            else -> getString(Res.string.error_adding_account, e.message ?: "")
        }
        stateManager.update { it.copy(snackbar = message) }
    }
}
