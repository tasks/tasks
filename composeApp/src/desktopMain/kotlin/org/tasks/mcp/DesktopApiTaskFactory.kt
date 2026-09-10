package org.tasks.mcp

import org.tasks.api.ApiTaskFactory
import org.tasks.data.TaskCreator
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.googleapis.DefaultListProvider
import org.tasks.preferences.AppPreferences

class DesktopApiTaskFactory(
    private val taskDao: TaskDao,
    private val caldavDao: CaldavDao,
    private val taskCreator: TaskCreator,
    private val taskSaver: TaskSaver,
    private val defaultListProvider: DefaultListProvider,
    private val appPreferences: AppPreferences,
) : ApiTaskFactory {

    override suspend fun defaultList(): CaldavFilter = defaultListProvider.getDefaultList()

    override suspend fun create(
        title: String,
        list: CaldavFilter,
        configure: (Task) -> Unit,
    ): Task {
        val task = taskCreator.createBlankTask(title = title).also(configure)
        val addToTop = appPreferences.addTasksToTop()
        taskDao.inTransaction {
            taskDao.createNew(task)
            caldavDao.insert(
                task = task,
                caldavTask = CaldavTask(task = task.id, calendar = list.uuid),
                addToTop = addToTop,
            )
        }
        taskSaver.save(task, null)
        return task
    }
}
