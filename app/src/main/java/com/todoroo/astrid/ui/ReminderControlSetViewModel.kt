package com.todoroo.astrid.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReminderControlSetViewModel : ViewModel() {
    private val _viewState = MutableStateFlow(ReminderControlSet.ViewState())

    val viewState: StateFlow<ReminderControlSet.ViewState>
        get() = _viewState.asStateFlow()

    fun showCustomDialog(visible: Boolean) {
        _viewState.value = _viewState.value.copy(
            showCustomDialog = visible
        )
    }

    fun showRandomDialog(visible: Boolean) {
        _viewState.value = _viewState.value.copy(
            showRandomDialog = visible
        )
    }
}