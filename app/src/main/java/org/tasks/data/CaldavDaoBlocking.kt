package org.tasks.data

import android.content.Context
import androidx.lifecycle.LiveData
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.data.Task
import kotlinx.coroutines.runBlocking
import org.tasks.filters.CaldavFilters
import org.tasks.time.DateTimeUtils.currentTimeMillis
import javax.inject.Inject

@Deprecated("use coroutines")
class CaldavDaoBlocking @Inject constructor(private val dao: CaldavDao) {
    fun subscribeToCalendars(): LiveData<List<CaldavCalendar>> {
        return dao.subscribeToCalendars()
    }

    fun getCalendarByUuid(uuid: String): CaldavCalendar? = runBlocking {
        dao.getCalendarByUuid(uuid)
    }

    fun getCalendarsByAccount(uuid: String): List<CaldavCalendar> = runBlocking {
        dao.getCalendarsByAccount(uuid)
    }

    fun getAccountByUuid(uuid: String): CaldavAccount? = runBlocking {
        dao.getAccountByUuid(uuid)
    }

    fun accountCount(): Int = runBlocking {
        dao.accountCount()
    }

    fun getAccounts(): List<CaldavAccount> = runBlocking {
        dao.getAccounts()
    }

    fun setCollapsed(id: Long, collapsed: Boolean) = runBlocking {
        dao.setCollapsed(id, collapsed)
    }

    fun insert(caldavAccount: CaldavAccount): Long = runBlocking {
        dao.insert(caldavAccount)
    }

    fun update(caldavAccount: CaldavAccount) = runBlocking {
        dao.update(caldavAccount)
    }

    fun insert(caldavCalendar: CaldavCalendar) = runBlocking {
        dao.insert(caldavCalendar)
    }

    fun insertInternal(caldavCalendar: CaldavCalendar): Long = runBlocking {
        dao.insertInternal(caldavCalendar)
    }

    fun update(caldavCalendar: CaldavCalendar) = runBlocking {
        dao.update(caldavCalendar)
    }

    fun insert(task: Task, caldavTask: CaldavTask, addToTop: Boolean): Long = runBlocking {
        dao.insert(task, caldavTask, addToTop)
    }

    internal fun findFirstTask(calendar: String, parent: Long): Long? = runBlocking {
        dao.findFirstTask(calendar, parent)
    }

    internal fun findLastTask(calendar: String, parent: Long): Long? = runBlocking {
        dao.findLastTask(calendar, parent)
    }

    fun insert(caldavTask: CaldavTask): Long = runBlocking {
        dao.insert(caldavTask)
    }

    fun insert(tasks: Iterable<CaldavTask>) = runBlocking {
        dao.insert(tasks)
    }

    fun update(caldavTask: CaldavTask) = runBlocking {
        dao.update(caldavTask)
    }

    fun update(caldavTask: SubsetCaldav) = runBlocking {
        dao.update(caldavTask)
    }

    internal fun update(id: Long, position: Long?, parent: String?) = runBlocking {
        dao.update(id, position, parent)
    }

    internal fun update(id: Long, position: Long?) = runBlocking {
        dao.update(id, position)
    }

    internal fun update(id: Long, remoteParent: String?) = runBlocking {
        dao.update(id, remoteParent)
    }

    fun update(tasks: Iterable<CaldavTask>) = runBlocking {
        dao.update(tasks)
    }

    fun delete(caldavTask: CaldavTask) = runBlocking {
        dao.delete(caldavTask)
    }

    fun getDeleted(calendar: String): List<CaldavTask> = runBlocking {
        dao.getDeleted(calendar)
    }

    fun markDeleted(tasks: List<Long>, now: Long = currentTimeMillis()) = runBlocking {
        dao.markDeleted(tasks, now)
    }

    fun getTask(taskId: Long): CaldavTask? = runBlocking {
        dao.getTask(taskId)
    }

    fun getRemoteIdForTask(taskId: Long): String? = runBlocking {
        dao.getRemoteIdForTask(taskId)
    }

    fun getTask(calendar: String, obj: String): CaldavTask? = runBlocking {
        dao.getTask(calendar, obj)
    }

    fun getTaskByRemoteId(calendar: String, remoteId: String): CaldavTask? = runBlocking {
        dao.getTaskByRemoteId(calendar, remoteId)
    }

    fun getTasks(taskId: Long): List<CaldavTask> = runBlocking {
        dao.getTasks(taskId)
    }

    fun getTasks(taskIds: List<Long>): List<CaldavTask> = runBlocking {
        dao.getTasks(taskIds)
    }

    fun getTasks(): List<CaldavTaskContainer> = runBlocking {
        dao.getTasks()
    }

    fun getCaldavTasksToPush(calendar: String): List<CaldavTaskContainer> = runBlocking {
        dao.getCaldavTasksToPush(calendar)
    }

    fun getCalendars(): List<CaldavCalendar> = runBlocking {
        dao.getCalendars()
    }

    fun getCalendar(uuid: String): CaldavCalendar? = runBlocking {
        dao.getCalendar(uuid)
    }

    fun getObjects(calendar: String): List<String> = runBlocking {
        dao.getObjects(calendar)
    }

    fun getTasks(calendar: String, objects: List<String>): List<Long> = runBlocking {
        dao.getTasks(calendar, objects)
    }

    fun getTasksInternal(calendar: String, objects: List<String>): List<Long> = runBlocking {
        dao.getTasksInternal(calendar, objects)
    }

    fun findDeletedCalendars(account: String, urls: List<String>): List<CaldavCalendar> = runBlocking {
        dao.findDeletedCalendars(account, urls)
    }

    fun getCalendarByUrl(account: String, url: String): CaldavCalendar? = runBlocking {
        dao.getCalendarByUrl(account, url)
    }

    fun getAccountForTask(task: Long): CaldavAccount? = runBlocking {
        dao.getAccountForTask(task)
    }

    fun getCalendars(tasks: List<Long>): List<String> = runBlocking {
        dao.getCalendars(tasks)
    }

    fun getCaldavFilters(uuid: String, now: Long = currentTimeMillis()): List<CaldavFilters> = runBlocking {
        dao.getCaldavFilters(uuid, now)
    }

    fun getTasksWithTags(): List<Long> = runBlocking {
        dao.getTasksWithTags()
    }

    fun updateParents() = runBlocking {
        dao.updateParents()
    }

    fun updateParents(calendar: String) = runBlocking {
        dao.updateParents(calendar)
    }

    fun move(task: TaskContainer, newParent: Long, newPosition: Long?) = runBlocking {
        dao.move(task, newParent, newPosition)
    }

    fun shiftDown(calendar: String, parent: Long, from: Long, to: Long? = null) = runBlocking {
        dao.shiftDown(calendar, parent, from, to)
    }

    internal fun touchInternal(ids: List<Long>, modificationTime: Long = now()) = runBlocking {
        dao.touchInternal(ids, modificationTime)
    }

    internal fun getTasksToShift(calendar: String, parent: Long, from: Long, to: Long?): List<CaldavTaskContainer> = runBlocking {
        dao.getTasksToShift(calendar, parent, from, to)
    }

    fun resetOrders() = runBlocking {
        dao.resetOrders()
    }

    fun setOrder(id: Long, order: Int) = runBlocking {
        dao.setOrder(id, order)
    }

    fun setupLocalAccount(context: Context): CaldavAccount = runBlocking {
        dao.setupLocalAccount(context)
    }

    fun getLocalList(context: Context) = runBlocking {
        dao.getLocalList(context)
    }
}