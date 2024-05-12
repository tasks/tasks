package com.todoroo.astrid.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.tasks.data.entity.Alarm

class ReminderControlSetViewModel : ViewModel() {

    data class ViewState(
        val showAddAlarm: Boolean = false,
        val showCustomDialog: Boolean = false,
        val showRandomDialog: Boolean = false,
        val replace: Alarm? = null,
    )

    private val _viewState = MutableStateFlow(ViewState())

    val viewState: StateFlow<ViewState>
        get() = _viewState.asStateFlow()

    fun setReplace(alarm: Alarm?) {
        _viewState.update { it.copy(replace = alarm) }
    }

    fun showAddAlarm(visible: Boolean) {
        _viewState.update { state ->
            state.copy(
                showAddAlarm = visible,
                replace = state.replace?.takeIf {
                    visible || state.showCustomDialog || state.showRandomDialog
                },
            )
        }
    }

    fun showCustomDialog(visible: Boolean) {
        _viewState.update { state ->
            state.copy(
                showCustomDialog = visible,
                replace = state.replace?.takeIf { visible }
            )
        }
    }

    fun showRandomDialog(visible: Boolean) {
        _viewState.update { state ->
            state.copy(
                showRandomDialog = visible,
                replace = state.replace?.takeIf { visible }
            )
        }
    }
}
