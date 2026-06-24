package org.tasks.data

import co.touchlab.kermit.Logger
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.db.DbUtils.dbchunk
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.preferences.AppPreferences
import org.tasks.service.TaskDeleter

class TaskMover(
    private val taskDao: TaskDao,
    private val caldavDao: CaldavDao,
    private val googleTaskDao: GoogleTaskDao,
    private val appPreferences: AppPreferences,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val taskDeleter: TaskDeleter,
) {
    private val log = Logger.withTag("TaskMover")

    suspend fun getSingleFilter(tasks: List<Long>): Filter? {
        val caldavCalendars = caldavDao.getCalendars(tasks)
        return if (caldavCalendars.size == 1) {
            val list = caldavCalendars.first()
            val account = list.account?.let { caldavDao.getAccountByUuid(it) }
            account?.let { CaldavFilter(calendar = list, account = it) }
        } else {
            null
        }
    }

    suspend fun move(task: Long, list: Long) {
        val calendar = caldavDao.getCalendarById(list) ?: return
        val account = calendar.account?.let { caldavDao.getAccountByUuid(it) } ?: return
        move(
            ids = listOf(task),
            selectedList = CaldavFilter(calendar = calendar, account = account),
        )
    }

    suspend fun move(task: Long, listUuid: String, newParent: Long) {
        val calendar = caldavDao.getCalendar(listUuid) ?: return
        val account = calendar.account?.let { caldavDao.getAccountByUuid(it) } ?: return
        move(
            ids = listOf(task),
            selectedList = CaldavFilter(calendar = calendar, account = account),
            newParent = newParent,
        )
    }

    suspend fun move(ids: List<Long>, selectedList: CaldavFilter, newParent: Long = 0L) {
        val tasks = ids
            .dbchunk()
            .flatMap { taskDao.getChildren(it) }
            .let { taskDao.fetch(ids.minus(it.toSet())) }
            .filterNot { it.readOnly }
        val taskIds = tasks.map { it.id }
        taskDao.inTransaction {
            taskDao.setParent(0, taskIds)
            tasks.forEach { performMove(it, selectedList) }
            if (newParent != 0L) {
                nestUnderParent(taskIds, newParent, selectedList)
            }
            if (selectedList.isIcalendar) {
                log.d { "Updating parents for ${selectedList.uuid}" }
                caldavDao.updateParents(selectedList.uuid, force = true)
            }
        }
        refreshBroadcaster.broadcastRefresh()
    }

    private suspend fun nestUnderParent(roots: List<Long>, newParent: Long, selectedList: CaldavFilter) {
        if (selectedList.isIcalendar) {
            val parentRemoteId = caldavDao.getTask(newParent)?.remoteId
            roots.forEach { root ->
                caldavDao.getTask(root)?.let { caldavDao.update(it.id, parentRemoteId) }
            }
            return
        }
        if (selectedList.isGoogleTasks) {
            roots.forEach { root ->
                taskDao.fetch(root)?.let {
                    googleTaskDao.move(
                        task = it,
                        list = selectedList.uuid,
                        newParent = newParent,
                        newPosition = googleTaskDao.getBottom(selectedList.uuid, newParent),
                    )
                }
            }
        } else {
            taskDao.setParent(newParent, roots)
        }
        taskDao.getChildren(roots).takeIf { it.isNotEmpty() }?.let {
            taskDao.setParent(newParent, it)
        }
    }

    suspend fun migrateLocalTasks() {
        val list = caldavDao.getLocalList()
        val account = list.account?.let { caldavDao.getAccountByUuid(it) } ?: return
        move(taskDao.getLocalTasks(), CaldavFilter(calendar = list, account = account))
    }

    private suspend fun performMove(task: Task, selectedList: CaldavFilter) {
        googleTaskDao.getByTaskId(task.id)?.let {
            moveGoogleTask(task, it, selectedList)
            return
        }
        caldavDao.getTask(task.id)?.let {
            moveCaldavTask(task, it, selectedList)
            return
        }
        moveLocalTask(task, selectedList)
    }

    private suspend fun moveGoogleTask(task: Task, googleTask: CaldavTask, selected: CaldavFilter) {
        if (googleTask.calendar == selected.uuid) {
            return
        }
        val id = task.id
        val children = taskDao.getChildren(id)
        taskDeleter.markMoved(children + id)
        when {
            selected.isGoogleTasks -> {
                val listId = selected.uuid
                googleTaskDao.insertAndShift(
                    task = task,
                    caldavTask = CaldavTask(
                        task = id,
                        calendar = listId,
                        remoteId = null,
                    ),
                    top = appPreferences.addTasksToTop()
                )
                children.takeIf { it.isNotEmpty() }
                        ?.map {
                            CaldavTask(
                                task = it,
                                calendar = listId,
                                remoteId = null,
                            )
                        }
                        ?.let { googleTaskDao.insert(it) }
            }
            else -> createCaldavSubtree(task, children, selected.uuid, flatten = selected.isSingleLevel)
        }
    }

    private suspend fun moveCaldavTask(task: Task, caldavTask: CaldavTask, selected: CaldavFilter) {
        if (caldavTask.calendar == selected.uuid) {
            return
        }
        val id = task.id
        val childIds = taskDao.getChildren(id)
        taskDeleter.markMoved(childIds + id)
        when {
            selected.isGoogleTasks -> moveToGoogleTasks(id, childIds, selected)
            else -> createCaldavSubtree(task, childIds, selected.uuid, flatten = selected.isSingleLevel)
        }
    }

    private suspend fun moveLocalTask(task: Task, selected: CaldavFilter) {
        when {
            selected.isGoogleTasks -> moveToGoogleTasks(task.id, taskDao.getChildren(task.id), selected)
            else -> createCaldavSubtree(task, taskDao.getChildren(task.id), selected.uuid, flatten = selected.isSingleLevel)
        }
    }

    private suspend fun createCaldavSubtree(task: Task, childIds: List<Long>, listId: String, flatten: Boolean) {
        val root = CaldavTask(task = task.id, calendar = listId)
        val caldavChildren = if (flatten) {
            taskDao.setParent(task.id, childIds)
            childIds.map { CaldavTask(task = it, calendar = listId).apply { remoteParent = root.remoteId } }
        } else {
            val byId = taskDao.fetch(childIds).associateBy { it.id }
            val remoteIds = hashMapOf(task.id to root.remoteId)
            childIds.mapNotNull { byId[it] }.map { child ->
                CaldavTask(task = child.id, calendar = listId)
                    .apply { remoteParent = remoteIds[child.parent] }
                    .also { remoteIds[child.id] = it.remoteId }
            }
        }
        caldavDao.insert(task, root, appPreferences.addTasksToTop())
        caldavChildren.takeIf { it.isNotEmpty() }?.let { caldavDao.insert(it) }
    }

    private suspend fun moveToGoogleTasks(id: Long, children: List<Long>, filter: CaldavFilter) {
        if (!filter.isGoogleTasks) {
            return
        }
        val task = taskDao.fetch(id) ?: return
        taskDao.setParent(id, children)
        val listId = filter.uuid
        googleTaskDao.insertAndShift(
            task,
            CaldavTask(
                task = id,
                calendar = listId,
                remoteId = null
            ),
            appPreferences.addTasksToTop()
        )
        children.takeIf { it.isNotEmpty() }
                ?.map {
                    CaldavTask(
                        task = it,
                        calendar = listId,
                        remoteId = null
                    )
                }
                ?.let { googleTaskDao.insert(it) }
    }
}
