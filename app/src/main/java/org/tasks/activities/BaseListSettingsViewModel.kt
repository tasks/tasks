package org.tasks.activities

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class BaseListSettingsViewModel : ViewModel() {
    data class ViewState(
        val title: String = "",
        val error: String = "",
        val showProgress: Boolean = false,
        val promptDelete: Boolean = false,
        val promptDiscard: Boolean = false,
        val icon: String? = null,
        val color: Int = 0,
    )

    private val _viewState = MutableStateFlow(ViewState())

    val viewState: StateFlow<ViewState> = _viewState

    fun setTitle(title: String) {
        _viewState.update { it.copy(title = title) }
    }

    fun setColor(color: Int) {
        _viewState.update {
            it.copy(color = color)
        }
    }

    fun setIcon(icon: String) {
        _viewState.update { it.copy(icon = icon) }
    }

    fun setError(error: String) {
        _viewState.update { it.copy(error = error) }
    }

    fun showProgress(showProgress: Boolean) {
        _viewState.update { it.copy(showProgress = showProgress) }
    }

    fun promptDiscard(promptDiscard: Boolean) {
        _viewState.update { it.copy(promptDiscard = promptDiscard) }
    }

    fun promptDelete(promptDelete: Boolean) {
        _viewState.update { it.copy(promptDelete = promptDelete) }
    }

    val title: String
        get() = _viewState.value.title

    val icon: String?
        get() = _viewState.value.icon

    val color: Int
        get() = _viewState.value.color
}
