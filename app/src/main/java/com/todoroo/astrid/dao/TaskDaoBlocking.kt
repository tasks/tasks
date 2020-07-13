/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao

import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.data.Task
import org.tasks.data.runBlocking
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

    fun snooze(taskIds: List<Long>, millis: Long) = runBlocking {
        dao.snooze(taskIds, millis)
    }

    fun getTasksWithReminders(): List<Task> = runBlocking {
        dao.getTasksWithReminders()
    }

    fun save(task: Task) = runBlocking {
        dao.save(task)
    }

    fun save(task: Task, original: Task? = fetchBlocking(task.id)) = runBlocking {
        dao.save(task, original)
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