package org.tasks.viewmodel

import kotlinx.coroutines.flow.StateFlow
import org.tasks.compose.settings.ListSettingsState
import org.tasks.data.PrincipalWithAccess

interface ListSettingsCallbacks {
    val state: StateFlow<ListSettingsState>

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
