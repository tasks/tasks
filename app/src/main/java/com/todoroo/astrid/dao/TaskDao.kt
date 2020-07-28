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
import org.tasks.data.TaskDao
import org.tasks.db.SuspendDbUtils.eachChunk
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import javax.inject.Inject

class TaskDao @Inject constructor(
        private val workManager: WorkManager,
        private val taskDao: TaskDao) {

    suspend fun fetch(id: Long): Task? = taskDao.fetch(id)

    suspend fun fetch(ids: List<Long>): List<Task> = taskDao.fetch(ids)

    suspend fun activeTimers(): Int = taskDao.activeTimers()

    suspend fun fetch(remoteId: String): Task? = taskDao.fetch(remoteId)

    suspend fun getRecurringTasks(remoteIds: List<String>): List<Task> =
            taskDao.getRecurringTasks(remoteIds)

    suspend fun setCompletionDate(remoteId: String, completionDate: Long) =
            taskDao.setCompletionDate(remoteId, completionDate)

    suspend fun getGoogleTasksToPush(account: String): List<Task> =
            taskDao.getGoogleTasksToPush(account)

    suspend fun getCaldavTasksToPush(calendar: String): List<Task> =
            taskDao.getCaldavTasksToPush(calendar)

    suspend fun fetchTasks(preferences: Preferences, filter: Filter): List<TaskContainer> =
            taskDao.fetchTasks(preferences, filter)

    suspend fun touch(id: Long) = touch(listOf(id))

    suspend fun touch(ids: List<Long>) {
        ids.eachChunk { taskDao.touch(ids) }
        workManager.sync(false)
    }

    suspend fun setParent(parent: Long, tasks: List<Long>) = taskDao.setParent(parent, tasks)

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
        if (taskDao.update(task, original)) {
            workManager.afterSave(task, original)
        }
    }

    suspend fun createNew(task: Task) = taskDao.createNew(task)

    suspend fun fetchFiltered(queryTemplate: String): List<Task> =
            taskDao.fetchFiltered(queryTemplate)

    internal suspend fun insert(task: Task): Long = taskDao.insert(task)

    internal suspend fun fetchTasks(callback: suspend (SubtaskInfo) -> List<String>): List<TaskContainer> =
            taskDao.fetchTasks(callback)

    internal suspend fun getAll(): List<Task> = taskDao.getAll()

    internal suspend fun getActiveTasks(): List<Task> = taskDao.getActiveTasks()
}