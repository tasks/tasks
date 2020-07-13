package org.tasks.data

import androidx.lifecycle.LiveData
import com.todoroo.astrid.data.Task
import javax.inject.Inject

@Deprecated("use coroutines")
class CaldavDaoBlocking @Inject constructor(private val dao: CaldavDao) {
    fun subscribeToCalendars(): LiveData<List<CaldavCalendar>> {
        return dao.subscribeToCalendars()
    }

    fun accountCount(): Int = runBlocking {
        dao.accountCount()
    }

    fun setCollapsed(id: Long, collapsed: Boolean) = runBlocking {
        dao.setCollapsed(id, collapsed)
    }

    fun insert(task: Task, caldavTask: CaldavTask, addToTop: Boolean): Long = runBlocking {
        dao.insert(task, caldavTask, addToTop)
    }

    fun getTask(taskId: Long): CaldavTask? = runBlocking {
        dao.getTask(taskId)
    }

    fun getCalendars(): List<CaldavCalendar> = runBlocking {
        dao.getCalendars()
    }
}