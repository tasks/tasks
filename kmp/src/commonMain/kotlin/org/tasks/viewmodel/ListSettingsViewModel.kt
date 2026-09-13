package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.billing.PurchaseState
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.service.TaskDeleter
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.name_cannot_be_empty

abstract class ListSettingsViewModel(
    protected val caldavDao: CaldavDao,
    protected val taskDeleter: TaskDeleter,
    protected val reporting: Reporting,
    purchaseState: PurchaseState,
    isDark: Boolean,
    account: CaldavAccount,
    calendar: CaldavCalendar,
    hasColorWheel: Boolean = false,
    internal val stateManager: ListSettingsStateManager =
        ListSettingsStateManager(isDark, purchaseState, account, calendar, hasColorWheel),
) : ViewModel(), ListSettingsCallbacks by stateManager {

    init {
        stateManager.observeTaskCount(viewModelScope, caldavDao)
    }

    open override fun setName(value: String) = stateManager.setName(value)
    open override fun setColor(value: Int) = stateManager.setColor(value)
    open override fun setIcon(value: String) = stateManager.setIcon(value)

    fun save(onDismiss: () -> Unit = {}, onComplete: (CaldavCalendar) -> Unit) {
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
                createList()?.let { onComplete(it) }
            } else if (s.hasChanges) {
                updateList()?.let { onComplete(it) }
            } else {
                onDismiss()
            }
        }
    }

    private suspend fun createList(): CaldavCalendar? {
        val s = state.value
        val account = s.account ?: return null
        val name = s.name.trim()
        stateManager.update { it.copy(isLoading = true) }
        return try {
            withContext(NonCancellable) {
                val calendar = withContext(Dispatchers.IO) {
                    createRemoteList(account, name, s.color, s.icon)
                }
                val id = caldavDao.insertOrReplace(calendar)
                reporting.logEvent(AnalyticsEvents.CREATE_LIST)
                val inserted = calendar.copy(id = id)
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

    private suspend fun updateList(): CaldavCalendar? {
        val s = state.value
        val account = s.account ?: return null
        val calendar = s.calendar ?: return null
        val name = s.name.trim()
        stateManager.update { it.copy(isLoading = true) }
        return try {
            withContext(NonCancellable) {
                val nameChanged = name != calendar.name
                if (nameChanged) {
                    withContext(Dispatchers.IO) {
                        renameRemoteList(account, calendar, name)
                    }
                }
                val result = calendar.copy(
                    name = name,
                    color = s.color,
                    icon = s.icon,
                )
                caldavDao.insertOrReplace(result)
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
                        deleteRemoteList(account, calendar)
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

    protected abstract suspend fun createRemoteList(
        account: CaldavAccount,
        name: String,
        color: Int,
        icon: String,
    ): CaldavCalendar

    protected abstract suspend fun renameRemoteList(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        name: String,
    )

    protected abstract suspend fun deleteRemoteList(
        account: CaldavAccount,
        calendar: CaldavCalendar,
    )

    protected abstract suspend fun handleError(e: Exception)
}
