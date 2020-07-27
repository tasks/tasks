/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao

import androidx.paging.DataSource
import androidx.sqlite.db.SimpleSQLiteQuery
import com.todoroo.andlib.sql.Criterion
import com.todoroo.andlib.sql.Functions
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.data.Task
import org.tasks.data.SubtaskInfo
import org.tasks.data.TaskContainer
import org.tasks.data.TaskDao
import org.tasks.db.SuspendDbUtils.eachChunk
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import javax.inject.Inject

class TaskDao @Inject constructor(
        private val workManager: WorkManager,
        private val taskDao: TaskDao) {

    internal suspend fun needsRefresh(now: Long = DateUtilities.now()): List<Task> =
            taskDao.needsRefresh(now)

    suspend fun fetch(id: Long): Task? = taskDao.fetch(id)

    suspend fun fetch(ids: List<Long>): List<Task> = taskDao.fetch(ids)

    suspend fun activeTimers(): Int = taskDao.activeTimers()

    suspend fun activeNotifications(): List<Task> = taskDao.activeNotifications()

    suspend fun fetch(remoteId: String): Task? = taskDao.fetch(remoteId)

    suspend fun getActiveTasks(): List<Task> = taskDao.getActiveTasks()

    suspend fun getRecurringTasks(remoteIds: List<String>): List<Task> =
            taskDao.getRecurringTasks(remoteIds)

    suspend fun setCompletionDate(remoteId: String, completionDate: Long) =
            taskDao.setCompletionDate(remoteId, completionDate)

    suspend fun snooze(taskIds: List<Long>, millis: Long) = taskDao.snooze(taskIds, millis)

    suspend fun getGoogleTasksToPush(account: String): List<Task> =
            taskDao.getGoogleTasksToPush(account)

    suspend fun getCaldavTasksToPush(calendar: String): List<Task> =
            taskDao.getCaldavTasksToPush(calendar)

    suspend fun getTasksWithReminders(): List<Task> = taskDao.getTasksWithReminders()

    suspend fun getAll(): List<Task> = taskDao.getAll()

    suspend fun getAllCalendarEvents(): List<String> = taskDao.getAllCalendarEvents()

    suspend fun clearAllCalendarEvents(): Int = taskDao.clearAllCalendarEvents()

    suspend fun getCompletedCalendarEvents(): List<String> = taskDao.getCompletedCalendarEvents()

    suspend fun clearCompletedCalendarEvents(): Int = taskDao.clearCompletedCalendarEvents()

    suspend fun fetchTasks(callback: suspend (SubtaskInfo) -> List<String>): List<TaskContainer> =
            taskDao.fetchTasks(callback)

    suspend fun fetchTasks(callback: suspend (SubtaskInfo) -> List<String>, subtasks: SubtaskInfo): List<TaskContainer> =
            taskDao.fetchTasks(callback, subtasks)

    suspend fun fetchTasks(preferences: Preferences, filter: Filter): List<TaskContainer> =
            taskDao.fetchTasks(preferences, filter)

    suspend fun fetchTasks(query: SimpleSQLiteQuery): List<TaskContainer> =
            taskDao.fetchTasks(query)

    suspend fun count(query: SimpleSQLiteQuery): Int = taskDao.count(query)

    suspend fun getSubtaskInfo(): SubtaskInfo = taskDao.getSubtaskInfo()

    fun getTaskFactory(query: SimpleSQLiteQuery): DataSource.Factory<Int, TaskContainer> =
            taskDao.getTaskFactory(query)

    suspend fun touch(id: Long) = touch(listOf(id))

    suspend fun touch(ids: List<Long>) {
        ids.eachChunk { taskDao.touch(ids) }
        workManager.sync(false)
    }

    suspend fun setParent(parent: Long, tasks: List<Long>) =
            tasks.eachChunk { setParentInternal(parent, it) }

    private suspend fun setParentInternal(parent: Long, children: List<Long>) =
            taskDao.setParentInternal(parent, children)

    suspend fun fetchChildren(id: Long): List<Task> = taskDao.fetchChildren(id)

    suspend fun getChildren(id: Long): List<Long> = taskDao.getChildren(id)

    suspend fun getChildren(ids: List<Long>): List<Long> = taskDao.getChildren(ids)

    suspend fun setCollapsed(id: Long, collapsed: Boolean) = taskDao.setCollapsed(id, collapsed)

    suspend fun setCollapsed(preferences: Preferences, filter: Filter, collapsed: Boolean) =
            taskDao.setCollapsed(preferences, filter, collapsed)

    // --- save
    // TODO: get rid of this super-hack
    /**
     * Saves the given task to the database.getDatabase(). Task must already exist. Returns true on
     * success.
     */

    suspend fun save(task: Task) = save(task, fetch(task.id))

    suspend fun save(task: Task, original: Task?) {
        if (!task.insignificantChange(original)) {
            task.modificationDate = DateUtilities.now()
        }
        if (task.dueDate != original?.dueDate) {
            task.reminderSnooze = 0
        }
        if (update(task) == 1) {
            workManager.afterSave(task, original)
        }
    }

    suspend fun insert(task: Task): Long = taskDao.insert(task)

    suspend fun update(task: Task): Int = taskDao.update(task)

    suspend fun createNew(task: Task) = taskDao.createNew(task)

    suspend fun count(filter: Filter): Int = taskDao.count(filter)

    suspend fun fetchFiltered(filter: Filter): List<Task> = taskDao.fetchFiltered(filter)

    suspend fun fetchFiltered(queryTemplate: String): List<Task> =
            taskDao.fetchFiltered(queryTemplate)

    suspend fun getLocalTasks(): List<Long> = taskDao.getLocalTasks()

    /** Generates SQL clauses  */
    object TaskCriteria {
        /** @return tasks that have not yet been completed or deleted
         */
        @JvmStatic fun activeAndVisible(): Criterion {
            return Criterion.and(
                    Task.COMPLETION_DATE.lte(0),
                    Task.DELETION_DATE.lte(0),
                    Task.HIDE_UNTIL.lte(Functions.now()))
        }
    }
}