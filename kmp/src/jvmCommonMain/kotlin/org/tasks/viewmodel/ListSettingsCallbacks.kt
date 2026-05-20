package org.tasks.viewmodel

import kotlinx.coroutines.flow.StateFlow
import org.tasks.compose.settings.ListSettingsState
import org.tasks.data.PrincipalWithAccess
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar

interface ListSettingsCallbacks {
    val state: StateFlow<ListSettingsState>

    fun setCalendar(account: CaldavAccount, calendar: CaldavCalendar?)
    fun setName(value: String)
    fun setColor(value: Int)
    fun setIcon(value: String)

    fun openColorPicker()
    fun closeColorPicker()
    fun selectColor(color: Int)

    fun openIconPicker()
    fun closeIconPicker()
    fun selectIcon(name: String)

    fun dismissSnackbar()
    fun showDiscardDialog()
    fun dismissDiscardDialog()

    fun openShareDialog() {}
    fun closeShareDialog() {}
    fun share(input: String) {}
    fun confirmRemovePrincipal(principal: PrincipalWithAccess?) {}
    fun removePrincipal(principal: PrincipalWithAccess) {}
}
