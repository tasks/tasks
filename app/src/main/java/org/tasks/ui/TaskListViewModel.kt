package org.tasks.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.Filter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.tasks.LocalBroadcastManager
import org.tasks.compose.throttleLatest
import org.tasks.data.TaskContainer
import org.tasks.data.TaskDao
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.preferences.Preferences
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(
        private val preferences: Preferences,
        private val taskDao: TaskDao,
        private val localBroadcastManager: LocalBroadcastManager,
) : ViewModel() {

    data class State(
        val filter: Filter? = null,
        val now: Long = DateUtilities.now(),
    )

    private val _state = MutableStateFlow(State())

    val tasks: Flow<List<TaskContainer>> =
        _state
            .filter { it.filter != null }
            .throttleLatest(333)
            .map { taskDao.fetchTasks { getQuery(preferences, it.filter!!) } }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            invalidate()
        }
    }

    fun setFilter(filter: Filter) {
        _state.update {
            it.copy(filter = filter)
        }
    }

    fun invalidate() {
        _state.update { it.copy(now = DateUtilities.now()) }
    }

    init {
        localBroadcastManager.registerRefreshReceiver(refreshReceiver)
    }

    override fun onCleared() {
        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }
}
