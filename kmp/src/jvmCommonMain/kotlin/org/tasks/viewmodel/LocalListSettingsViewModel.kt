package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import org.tasks.service.TaskDeleter
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.name_cannot_be_empty

open class LocalListSettingsViewModel(
    private val caldavDao: CaldavDao,
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

    fun save(onDismiss: () -> Unit = {}, onComplete: (CaldavAccount, CaldavCalendar) -> Unit) {
        if (state.value.isLoading) return
        viewModelScope.launch {
            val s = state.value
            val name = s.name.trim()
            if (name.isEmpty()) {
                val error = getString(Res.string.name_cannot_be_empty)
                stateManager.update { it.copy(nameError = error) }
                return@launch
            }
            val account = s.account ?: return@launch
            if (s.isNew) {
                createCalendar()?.let { onComplete(account, it) }
            } else if (s.hasChanges) {
                updateCalendar()?.let { onComplete(account, it) }
            } else {
                onDismiss()
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
                val calendar = CaldavCalendar(
                    uuid = UUIDHelper.newUUID(),
                    account = account.uuid,
                    name = name,
                    color = s.color,
                    icon = s.icon,
                )
                caldavDao.insert(calendar)
                reporting.logEvent(AnalyticsEvents.CREATE_LIST)
                stateManager.update { it.copy(calendar = calendar) }
                calendar
            }
        } finally {
            stateManager.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun updateCalendar(): CaldavCalendar? {
        val s = state.value
        val calendar = s.calendar ?: return null
        val name = s.name.trim()
        stateManager.update { it.copy(isLoading = true) }
        return try {
            withContext(NonCancellable) {
                val result = calendar.copy(
                    name = name,
                    color = s.color,
                    icon = s.icon,
                )
                caldavDao.insertOrReplace(result)
                stateManager.update { it.copy(calendar = result) }
                result
            }
        } finally {
            stateManager.update { it.copy(isLoading = false) }
        }
    }

    fun delete(onComplete: () -> Unit) {
        val s = state.value
        val calendar = s.calendar ?: return
        if (s.isLoading) return
        viewModelScope.launch {
            stateManager.update { it.copy(isLoading = true) }
            try {
                withContext(NonCancellable) {
                    reporting.logEvent(
                        AnalyticsEvents.SETTINGS_CLICK,
                        AnalyticsEvents.PARAM_TYPE to AnalyticsEvents.SettingsClick.DELETE_LIST,
                    )
                    taskDeleter.delete(calendar)
                }
                onComplete()
            } finally {
                stateManager.update { it.copy(isLoading = false) }
            }
        }
    }
}
