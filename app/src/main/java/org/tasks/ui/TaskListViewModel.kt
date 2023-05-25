package org.tasks.ui

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.api.Filter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.tasks.data.TaskContainer
import org.tasks.data.TaskDao
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(
        private val preferences: Preferences,
        private val taskDao: TaskDao) : ViewModel() {

    private var _tasks = MutableLiveData<List<TaskContainer>>()
    val tasks: LiveData<List<TaskContainer>>
        get() = _tasks
    private var filter: Filter? = null
    private var manualSortFilter = false

    fun setFilter(filter: Filter) {
        manualSortFilter = (filter.supportsManualSort() && preferences.isManualSort
                || filter.supportsAstridSorting() && preferences.isAstridSort)
        if (filter != this.filter || filter.getSqlQuery() != this.filter!!.getSqlQuery()) {
            this.filter = filter
            _tasks = MutableLiveData()
            invalidate()
        }
    }

    fun observe(owner: LifecycleOwner, observer: (List<TaskContainer>) -> Unit) =
            _tasks.observe(owner, observer)

    fun searchByFilter(filter: Filter?) {
        this.filter = filter
        invalidate()
    }

    fun invalidate() {
        AndroidUtilities.assertMainThread()
        if (filter == null) {
            return
        }
        try {
            viewModelScope.launch {
                _tasks.value = taskDao.fetchTasks { getQuery(preferences, filter!!) }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    val value: List<TaskContainer>
        get() = _tasks.value ?: emptyList()
}