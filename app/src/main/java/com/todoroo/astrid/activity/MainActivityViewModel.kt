package com.todoroo.astrid.activity

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoroo.astrid.activity.MainActivity.Companion.LOAD_FILTER
import com.todoroo.astrid.activity.MainActivity.Companion.OPEN_FILTER
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.Task
import org.tasks.filters.Filter
import org.tasks.filters.SearchFilter
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.viewmodel.DrawerViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val defaultFilterProvider: DefaultFilterProvider,
    @ApplicationContext private val applicationContext: Context,
    private val caldavDao: CaldavDao,
    private val accountDataRepository: TasksAccountDataRepository,
    val drawerViewModel: DrawerViewModel,
) : ViewModel() {

    data class State(
        val filter: Filter,
        val task: Task? = null,
    )

    private val _state = MutableStateFlow(
        State(
            filter = savedStateHandle.get<Filter>(OPEN_FILTER)
                ?: savedStateHandle.get<String>(LOAD_FILTER)?.let {
                    runBlocking { defaultFilterProvider.getFilterFromPreference(it) }
                }
                ?: runBlocking { defaultFilterProvider.getStartupFilter() },
            task = savedStateHandle.get<Task>(EXTRA_TASK),
        )
    )
    val state = _state.asStateFlow()

    companion object {
        private const val EXTRA_TASK = "extra_task"
    }

    val accountExists: Flow<Boolean>
        get() = caldavDao.watchAccountExists()

    init {
        drawerViewModel.setSelectedFilter(_state.value.filter)
    }

    suspend fun resetFilter() {
        setFilter(defaultFilterProvider.getDefaultOpenFilter())
    }

    fun setFilter(
        filter: Filter,
        task: Task? = null,
    ) {
        if (filter == _state.value.filter && task == null) {
            return
        }
        savedStateHandle[EXTRA_TASK] = task
        _state.update {
            it.copy(
                filter = filter,
                task = task,
            )
        }
        drawerViewModel.setSelectedFilter(filter)
        if (filter !is SearchFilter) {
            defaultFilterProvider.setLastViewedFilter(filter)
        }
    }

    fun setTask(task: Task?) {
        savedStateHandle[EXTRA_TASK] = task
        _state.update { it.copy(task = task) }
    }

    suspend fun getAccount(id: Long) = caldavDao.getAccount(id)

    suspend fun isTasksGuest(): Boolean =
        try {
            val response = accountDataRepository.getAccountResponse()
                ?: accountDataRepository.fetchAndCache()
            response?.guest ?: false
        } catch (e: Exception) {
            Timber.e(e, "Failed to check guest status")
            false
        }

    fun openLastViewedFilter() = viewModelScope.launch {
        setFilter(defaultFilterProvider.getLastViewedFilter())
    }
}
