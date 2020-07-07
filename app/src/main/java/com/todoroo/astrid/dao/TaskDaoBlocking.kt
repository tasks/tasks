/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao

import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.data.Task
import org.tasks.data.SubtaskInfo
import org.tasks.data.TaskContainer
import org.tasks.data.runBlocking
import org.tasks.preferences.Preferences
import javax.inject.Inject

@Deprecated("use coroutines")
class TaskDaoBlocking @Inject constructor(private val dao: TaskDao) {
    fun needsRefresh(): List<Task> = runBlocking {
        dao.needsRefresh()
    }

    fun fetchBlocking(id: Long) = runBlocking {
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
        dao.fetchTasks {
            callback.invoke(it)
        }
    }

    fun fetchTasks(preferences: Preferences, filter: Filter): List<TaskContainer> = runBlocking {
        dao.fetchTasks(preferences, filter)
    }

    fun touch(ids: List<Long>) = runBlocking {
        dao.touch(ids)
    }

    fun getChildren(ids: List<Long>): List<Long> = runBlocking {
        dao.getChildren(ids)
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
}