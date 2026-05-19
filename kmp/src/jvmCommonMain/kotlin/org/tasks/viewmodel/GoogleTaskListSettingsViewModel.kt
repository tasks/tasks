package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.billing.PurchaseState
import org.tasks.compose.settings.CaldavCalendarSettingsState
import org.tasks.compose.settings.buildPickerColors
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.googleapis.GtasksInvoker
import org.tasks.service.TaskDeleter
import org.tasks.themes.TasksIcons
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.gtasks_GLA_errorIOAuth
import tasks.kmp.generated.resources.name_cannot_be_empty

open class GoogleTaskListSettingsViewModel(
    private val caldavDao: CaldavDao,
    private val taskDeleter: TaskDeleter,
    private val reporting: Reporting,
    private val purchaseState: PurchaseState,
    private val invokerFactory: suspend (CaldavAccount) -> GtasksInvoker,
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

    fun save(onDismiss: () -> Unit = {}, onComplete: (CaldavCalendar) -> Unit) {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            val s = _state.value
            val name = s.name.trim()
            if (name.isEmpty()) {
                _state.update { it.copy(nameError = getString(Res.string.name_cannot_be_empty)) }
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
        val s = _state.value
        val account = s.account ?: return null
        val name = s.name.trim()
        _state.update { it.copy(isLoading = true) }
        return try {
            withContext(NonCancellable) {
                val taskList = withContext(Dispatchers.IO) {
                    invokerFactory(account).createGtaskList(name)!!
                }
                val calendar = CaldavCalendar(
                    uuid = taskList.id,
                    account = account.username,
                    name = taskList.title,
                    color = s.color,
                    icon = s.icon,
                )
                val id = caldavDao.insertOrReplace(calendar)
                reporting.logEvent(AnalyticsEvents.CREATE_LIST)
                val inserted = calendar.copy(id = id)
                _state.update { it.copy(calendar = inserted) }
                inserted
            }
        } catch (e: Exception) {
            handleError(e)
            null
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun updateList(): CaldavCalendar? {
        val s = _state.value
        val account = s.account ?: return null
        val calendar = s.calendar ?: return null
        val name = s.name.trim()
        _state.update { it.copy(isLoading = true) }
        return try {
            withContext(NonCancellable) {
                val nameChanged = name != calendar.name
                if (nameChanged) {
                    withContext(Dispatchers.IO) {
                        invokerFactory(account).renameGtaskList(calendar.uuid, name)
                    }
                }
                val result = calendar.copy(
                    name = name,
                    color = s.color,
                    icon = s.icon,
                )
                caldavDao.insertOrReplace(result)
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
        if (s.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                withContext(NonCancellable) {
                    withContext(Dispatchers.IO) {
                        invokerFactory(account).deleteGtaskList(calendar.uuid)
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
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun handleError(e: Exception) {
        Logger.e(e) { "Google Tasks list operation failed" }
        _state.update {
            it.copy(snackbar = getString(Res.string.gtasks_GLA_errorIOAuth))
        }
    }

}
