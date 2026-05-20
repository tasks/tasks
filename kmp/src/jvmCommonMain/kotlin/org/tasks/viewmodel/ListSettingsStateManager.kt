package org.tasks.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.tasks.billing.PurchaseState
import org.tasks.compose.settings.ListSettingsState
import org.tasks.compose.settings.buildPickerColors
import org.tasks.data.PrincipalWithAccess
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.themes.TasksIcons

class ListSettingsStateManager(
    isDark: Boolean,
    private val purchaseState: PurchaseState,
    hasColorWheel: Boolean = false,
) : ListSettingsCallbacks {

    private val pickerColors = buildPickerColors(isDark)

    private val _state = MutableStateFlow(
        ListSettingsState(
            pickerColors = pickerColors,
            hasPro = purchaseState.purchasedThemes(),
            hasColorWheel = hasColorWheel,
        )
    )
    override val state: StateFlow<ListSettingsState> = _state

    fun update(transform: (ListSettingsState) -> ListSettingsState) {
        _state.update(transform)
    }

    override fun setCalendar(account: CaldavAccount, calendar: CaldavCalendar?) {
        if (_state.value.account != null) return
        _state.value = ListSettingsState(
            name = calendar?.name ?: "",
            color = calendar?.color ?: 0,
            icon = calendar?.icon ?: TasksIcons.LIST,
            calendar = calendar,
            account = account,
            pickerColors = pickerColors,
            hasPro = purchaseState.purchasedThemes(),
            hasColorWheel = _state.value.hasColorWheel,
        )
    }

    override fun setName(value: String) {
        _state.update { it.copy(name = value, nameError = null) }
    }

    override fun setColor(value: Int) {
        _state.update { it.copy(color = value) }
    }

    override fun setIcon(value: String) {
        _state.update { it.copy(icon = value) }
    }

    override fun openColorPicker() {
        _state.update { it.copy(showColorPicker = true, hasPro = purchaseState.purchasedThemes()) }
    }

    override fun closeColorPicker() {
        _state.update { it.copy(showColorPicker = false) }
    }

    override fun selectColor(color: Int) {
        _state.update { it.copy(color = color, showColorPicker = false) }
    }

    override fun openIconPicker() {
        _state.update { it.copy(showIconPicker = true, hasPro = purchaseState.purchasedThemes()) }
    }

    override fun closeIconPicker() {
        _state.update { it.copy(showIconPicker = false) }
    }

    override fun selectIcon(name: String) {
        _state.update { it.copy(icon = name, showIconPicker = false) }
    }

    override fun dismissSnackbar() {
        _state.update { it.copy(snackbar = null) }
    }

    override fun showDiscardDialog() {
        _state.update { it.copy(showDiscardDialog = true) }
    }

    override fun dismissDiscardDialog() {
        _state.update { it.copy(showDiscardDialog = false) }
    }
}
