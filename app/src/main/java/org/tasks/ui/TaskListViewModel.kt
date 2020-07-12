package org.tasks.ui

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.sqlite.db.SimpleSQLiteQuery
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.dao.TaskDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tasks.BuildConfig
import org.tasks.data.SubtaskInfo
import org.tasks.data.TaskContainer
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.preferences.Preferences
import timber.log.Timber

class TaskListViewModel @ViewModelInject constructor(
        private val preferences: Preferences,
        private val taskDao: TaskDao) : ViewModel(), Observer<PagedList<TaskContainer>> {

    private var tasks = MutableLiveData<List<TaskContainer>>()
    private var filter: Filter? = null
    private var manualSortFilter = false
    private var internal: LiveData<PagedList<TaskContainer>>? = null

    fun setFilter(filter: Filter) {
        if (filter != this.filter
                || filter.getSqlQuery() != this.filter!!.getSqlQuery()) {
            this.filter = filter
            tasks = MutableLiveData()
            invalidate()
        }
        manualSortFilter = (filter.supportsManualSort() && preferences.isManualSort
                || filter.supportsAstridSorting() && preferences.isAstridSort)
    }

    fun observe(owner: LifecycleOwner, observer: (List<TaskContainer>) -> Unit) =
            tasks.observe(owner, observer)

    fun searchByFilter(filter: Filter?) {
        this.filter = filter
        invalidate()
    }

    private fun removeObserver() = internal?.removeObserver(this)

    fun invalidate() {
        AndroidUtilities.assertMainThread()
        removeObserver()
        if (filter == null) {
            return
        }
        try {
            viewModelScope.launch {
                val subtasks = taskDao.getSubtaskInfo()
                if (manualSortFilter || !preferences.usePagedQueries()) {
                    performNonPagedQuery(subtasks)
                } else {
                    performPagedListQuery()
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private suspend fun performNonPagedQuery(subtasks: SubtaskInfo) {
        tasks.value = taskDao.fetchTasks(
                { s: SubtaskInfo? -> getQuery(preferences, filter!!, s!!) },
                subtasks)
    }

    private fun performPagedListQuery() {
        val queries = getQuery(preferences, filter!!, SubtaskInfo())
        if (BuildConfig.DEBUG && queries.size != 1) {
            throw RuntimeException("Invalid queries")
        }
        val query = SimpleSQLiteQuery(queries[0])
        Timber.d("paged query: %s", query.sql)
        val factory = taskDao.getTaskFactory(query)
        val builder = LivePagedListBuilder(factory, PAGED_LIST_CONFIG)
        val current = tasks.value
        if (current is PagedList<*>) {
            val lastKey = (current as PagedList<TaskContainer>).lastKey
            if (lastKey is Int) {
                builder.setInitialLoadKey(lastKey as Int?)
            }
        }
        if (BuildConfig.DEBUG) {
            builder.setFetchExecutor { command: Runnable ->
                viewModelScope.launch(Dispatchers.IO) {
                    val start = DateUtilities.now()
                    command.run()
                    Timber.d("*** paged list execution took %sms", DateUtilities.now() - start)
                }
            }
        }
        internal = builder.build()
        internal!!.observeForever(this)
    }

    override fun onCleared() {
        removeObserver()
    }

    val value: List<TaskContainer>
        get() = tasks.value ?: emptyList()

    override fun onChanged(taskContainers: PagedList<TaskContainer>) {
        tasks.value = taskContainers
    }

    companion object {
        private val PAGED_LIST_CONFIG = PagedList.Config.Builder().setPageSize(20).build()
    }
}