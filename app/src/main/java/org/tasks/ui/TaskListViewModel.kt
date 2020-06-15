package org.tasks.ui

import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.sqlite.db.SimpleSQLiteQuery
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.dao.TaskDao
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.tasks.BuildConfig
import org.tasks.data.SubtaskInfo
import org.tasks.data.TaskContainer
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject

class TaskListViewModel : ViewModel(), Observer<PagedList<TaskContainer>> {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var taskDao: TaskDao

    private var tasks = MutableLiveData<List<TaskContainer>>()
    private var filter: Filter? = null
    private var manualSortFilter = false
    private val disposable = CompositeDisposable()
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

    fun observe(owner: LifecycleOwner, observer: Observer<List<TaskContainer>>) =
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
        disposable.add(
                Single.fromCallable { taskDao.getSubtaskInfo() }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { subtasks: SubtaskInfo ->
                                    if (manualSortFilter || !preferences.usePagedQueries()) {
                                        performNonPagedQuery(subtasks)
                                    } else {
                                        performPagedListQuery()
                                    }
                                }) { t: Throwable? -> Timber.e(t) })
    }

    private fun performNonPagedQuery(subtasks: SubtaskInfo) =
            disposable.add(
                    Single.fromCallable { taskDao.fetchTasks({ s: SubtaskInfo? -> getQuery(preferences, filter!!, s!!) }, subtasks) }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ value: List<TaskContainer> -> tasks.postValue(value) }) { t: Throwable? -> Timber.e(t) })

    private fun performPagedListQuery() {
        val queries = getQuery(preferences, filter!!, SubtaskInfo())
        if (BuildConfig.DEBUG && queries.size != 1) {
            throw RuntimeException("Invalid queries")
        }
        val query = SimpleSQLiteQuery(queries[0])
        Timber.d("paged query: %s", query.sql)
        val factory = taskDao.getTaskFactory(query)
        val builder = LivePagedListBuilder(factory, PAGED_LIST_CONFIG)
        val current = tasks.value!!
        if (current is PagedList<*>) {
            val lastKey = (current as PagedList<TaskContainer>).lastKey
            if (lastKey is Int) {
                builder.setInitialLoadKey(lastKey as Int?)
            }
        }
        if (BuildConfig.DEBUG) {
            builder.setFetchExecutor { command: Runnable ->
                Completable.fromAction {
                    AndroidUtilities.assertNotMainThread()
                    val start = DateUtilities.now()
                    command.run()
                    Timber.d("*** paged list execution took %sms", DateUtilities.now() - start)
                }
                        .subscribeOn(Schedulers.io())
                        .subscribe()
            }
        }
        internal = builder.build()
        internal!!.observeForever(this)
    }

    override fun onCleared() {
        disposable.dispose()
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