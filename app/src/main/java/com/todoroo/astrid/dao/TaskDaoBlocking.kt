/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao

import androidx.paging.DataSource
import androidx.sqlite.db.SimpleSQLiteQuery
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.data.Task
import kotlinx.coroutines.runBlocking
import org.tasks.data.SubtaskInfo
import org.tasks.data.TaskContainer
import org.tasks.preferences.Preferences
import javax.inject.Inject

@Deprecated("use coroutines")
class TaskDaoBlocking @Inject constructor(private val dao: TaskDao) {
    fun needsRefresh(): List<Task> = runBlocking {
        dao.needsRefresh()
    }

    fun fetchBlocking(id: Long) = runBlocking {
        dao.fetchBlocking(id)
    }

    fun fetch(id: Long): Task? = runBlocking {
        dao.fetch(id)
    }

    fun fetch(ids: List<Long>): List<Task> = runBlocking {
        dao.fetch(ids)
    }

    fun activeTimers(): Int = runBlocking {
        dao.activeTimers()
    }

    fun activeNotifications(): List<Task> = runBlocking {
        dao.activeNotifications()
    }

    fun fetch(remoteId: String): Task? = runBlocking {
        dao.fetch(remoteId)
    }

    fun getActiveTasks(): List<Task> = runBlocking {
        dao.getActiveTasks()
    }

    fun getRecurringTasks(remoteIds: List<String>): List<Task> = runBlocking {
        dao.getRecurringTasks(remoteIds)
    }

    fun setCompletionDate(remoteId: String, completionDate: Long) = runBlocking {
        dao.setCompletionDate(remoteId, completionDate)
    }

    fun snooze(taskIds: List<Long>, millis: Long) = runBlocking {
        dao.snooze(taskIds, millis)
    }

    fun getGoogleTasksToPush(account: String): List<Task> = runBlocking {
        dao.getGoogleTasksToPush(account)
    }

    fun getCaldavTasksToPush(calendar: String): List<Task> = runBlocking {
        dao.getCaldavTasksToPush(calendar)
    }

    fun getTasksWithReminders(): List<Task> = runBlocking {
        dao.getTasksWithReminders()
    }

    fun getAll(): List<Task> = runBlocking {
        dao.getAll()
    }

    fun getAllCalendarEvents(): List<String> = runBlocking {
        dao.getAllCalendarEvents()
    }

    fun clearAllCalendarEvents(): Int = runBlocking {
        dao.clearAllCalendarEvents()
    }

    fun getCompletedCalendarEvents(): List<String> = runBlocking {
        dao.getCompletedCalendarEvents()
    }

    fun clearCompletedCalendarEvents(): Int = runBlocking {
        dao.clearCompletedCalendarEvents()
    }

    fun fetchTasks(callback: (SubtaskInfo) -> List<String>): List<TaskContainer> = runBlocking {
        dao.fetchTasks(callback)
    }

    fun fetchTasks(callback: (SubtaskInfo) -> List<String>, subtasks: SubtaskInfo): List<TaskContainer> = runBlocking {
        dao.fetchTasks(callback, subtasks)
    }

    fun fetchTasks(preferences: Preferences, filter: Filter): List<TaskContainer> = runBlocking {
        dao.fetchTasks(preferences, filter)
    }

    fun fetchTasks(query: SimpleSQLiteQuery): List<TaskContainer> = runBlocking {
        dao.fetchTasks(query)
    }

    fun count(query: SimpleSQLiteQuery): Int = runBlocking {
        dao.count(query)
    }

    fun getSubtaskInfo(): SubtaskInfo = runBlocking {
        dao.getSubtaskInfo()
    }

    fun getTaskFactory(query: SimpleSQLiteQuery): DataSource.Factory<Int, TaskContainer> {
        return dao.getTaskFactory(query)
    }

    fun touch(id: Long) = runBlocking {
        dao.touch(id)
    }

    fun touch(ids: List<Long>) = runBlocking {
        dao.touch(ids)
    }

    fun setParent(parent: Long, tasks: List<Long>) = runBlocking {
        dao.setParent(parent, tasks)
    }

    fun fetchChildren(id: Long): List<Task> = runBlocking {
        dao.fetchChildren(id)
    }

    fun getChildren(id: Long): List<Long> = runBlocking {
        dao.getChildren(id)
    }

    fun getChildren(ids: List<Long>): List<Long> = runBlocking {
        dao.getChildren(ids)
    }

    fun setCollapsed(id: Long, collapsed: Boolean) = runBlocking {
        dao.setCollapsed(id, collapsed)
    }

    fun setCollapsed(preferences: Preferences, filter: Filter, collapsed: Boolean) = runBlocking {
        dao.setCollapsed(preferences, filter, collapsed)
    }

    fun save(task: Task) = runBlocking {
        dao.save(task)
    }

    fun save(task: Task, original: Task? = fetchBlocking(task.id)) = runBlocking {
        dao.save(task, original)
    }

    fun insert(task: Task): Long = runBlocking {
        dao.insert(task)
    }

    fun update(task: Task): Int = runBlocking {
        dao.update(task)
    }

    fun createNew(task: Task) = runBlocking {
        dao.createNew(task)
    }

    fun count(filter: Filter): Int = runBlocking {
        dao.count(filter)
    }

    fun fetchFiltered(filter: Filter): List<Task> = runBlocking {
        dao.fetchFiltered(filter)
    }

    fun fetchFiltered(queryTemplate: String): List<Task> = runBlocking {
        dao.fetchFiltered(queryTemplate)
    }

    fun getLocalTasks(): List<Long> = runBlocking {
        dao.getLocalTasks()
    }
}