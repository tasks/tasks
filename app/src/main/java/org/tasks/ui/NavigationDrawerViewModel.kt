package org.tasks.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.data.TaskDao
import org.tasks.filters.FilterProvider
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NavigationDrawerViewModel @Inject constructor(
    private val filterProvider: FilterProvider,
    private val taskDao: TaskDao,
    private val localBroadcastManager: LocalBroadcastManager,
) : ViewModel() {
    data class ViewState(
        val selected: Filter? = null,
        val filters: List<FilterListItem> = emptyList(),
    )

    private val _viewState = MutableStateFlow(ViewState())
    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                LocalBroadcastManager.REFRESH,
                LocalBroadcastManager.REFRESH_LIST -> updateFilters()
            }
        }
    }

    val viewState: StateFlow<ViewState>
        get() = _viewState.asStateFlow()

    fun setSelected(filter: Filter?) {
        _viewState.update { it.copy(selected = filter) }
    }

    fun updateFilters() = viewModelScope.launch {
        filterProvider
            .navDrawerItems()
            .onEach {
                if (it is Filter && it.count == -1) {
                    it.count = try {
                        taskDao.count(it)
                    } catch (e: Exception) {
                        Timber.e(e)
                        0
                    }
                }
            }
            .let { filters -> _viewState.update { it.copy(filters = filters) } }
    }

    override fun onCleared() {
        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }

    init {
        localBroadcastManager.registerRefreshListReceiver(refreshReceiver)
        updateFilters()
    }
}