package com.todoroo.astrid.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReminderControlSetViewModel : ViewModel() {

    data class ViewState(
        val showCustomDialog: Boolean = false,
        val showRandomDialog: Boolean = false,
    )

    private val _viewState = MutableStateFlow(ViewState())

    val viewState: StateFlow<ViewState>
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