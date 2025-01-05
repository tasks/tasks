package com.todoroo.astrid.service

import org.tasks.LocalBroadcastManager
import org.tasks.caldav.VtodoCache
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.db.DbUtils.dbchunk
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.getLocalList
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.preferences.Preferences
import org.tasks.sync.SyncAdapters
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import javax.inject.Inject

class TaskMover @Inject constructor(
    private val taskDao: TaskDao,
    private val caldavDao: CaldavDao,
    private val googleTaskDao: GoogleTaskDao,
    private val preferences: Preferences,
    private val localBroadcastManager: LocalBroadcastManager,
    private val syncAdapters: SyncAdapters,
    private val vtodoCache: VtodoCache,
) {

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

    suspend fun move(ids: List<Long>, selectedList: CaldavFilter) {
        val tasks = ids
            .dbchunk()
            .flatMap { taskDao.getChildren(it) }
            .let { taskDao.fetch(ids.minus(it.toSet())) }
            .filterNot { it.readOnly }
        val taskIds = tasks.map { it.id }
        taskDao.setParent(0, ids.intersect(taskIds.toSet()).toList())
        tasks.forEach { performMove(it, selectedList) }
        if (!selectedList.isGoogleTasks) {
            caldavDao.updateParents(selectedList.uuid)
        }
        taskIds.dbchunk().forEach {
            taskDao.touch(it)
        }
        localBroadcastManager.broadcastRefresh()
        syncAdapters.sync()
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
        caldavDao.markDeleted(children + id, currentTimeMillis())
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
                    top = preferences.addTasksToTop()
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
            else -> {
                val listId = selected.uuid
                val newParent = CaldavTask(
                    task = id,
                    calendar = listId,
                )
                caldavDao.insert(task, newParent, preferences.addTasksToTop())
                children.map {
                    val newChild = CaldavTask(
                        task = it,
                        calendar = listId
                    )
                    newChild.remoteParent = newParent.remoteId
                    newChild
                }.let { caldavDao.insert(it) }
            }
        }
    }

    private suspend fun moveCaldavTask(task: Task, caldavTask: CaldavTask, selected: CaldavFilter) {
        if (caldavTask.calendar == selected.uuid) {
            // TODO: make sure its the same account
            return
        }
        val id = task.id
        val childIds = taskDao.getChildren(id)
        val toDelete = arrayListOf(id)
        var children: List<CaldavTask> = emptyList()
        if (childIds.isNotEmpty()) {
            children = caldavDao.getTasks(childIds)
            toDelete.addAll(childIds)
        }
        caldavDao.markDeleted(toDelete, currentTimeMillis())
        when {
            selected.isGoogleTasks -> moveToGoogleTasks(id, childIds, selected)
            else -> {
                val from = caldavDao.getCalendar(caldavTask.calendar!!)
                val id1 = caldavTask.task
                val listId = selected.uuid
                val newParent = CaldavTask(
                    task = id1,
                    calendar = listId,
                    remoteId = caldavTask.remoteId,
                    obj = caldavTask.obj,
                )
                vtodoCache.move(from!!, selected.calendar, caldavTask)
                caldavDao.insert(task, newParent, preferences.addTasksToTop())
                children.takeIf { it.isNotEmpty() }
                        ?.map {
                            val newChild = CaldavTask(
                                task = it.task,
                                calendar = listId,
                                remoteId = it.remoteId,
                                obj = it.obj,
                            )
                            vtodoCache.move(from, selected.calendar, it)
                            newChild.remoteParent = it.remoteParent
                            newChild
                        }
                        ?.let { caldavDao.insert(it) }
            }
        }
    }

    private suspend fun moveLocalTask(task: Task, selected: CaldavFilter) {
        when {
            selected.isGoogleTasks -> moveToGoogleTasks(task.id, taskDao.getChildren(task.id), selected)
            else -> {
                val id = task.id
                val listId = selected.uuid
                val tasks: MutableMap<Long, CaldavTask> = HashMap()
                val root = CaldavTask(
                    task = id,
                    calendar = listId,
                )
                val children = taskDao.getChildren(id).mapNotNull { taskDao.fetch(it) }
                for (child in children) {
                    val newTask = CaldavTask(
                        task = child.id,
                        calendar = listId,
                    )
                    val parent = child.parent
                    newTask.remoteParent = (if (parent == id) root else tasks[parent])!!.remoteId
                    tasks[child.id] = newTask
                }
                caldavDao.insert(task, root, preferences.addTasksToTop())
                caldavDao.insert(tasks.values)
            }
        }
    }

    private suspend fun moveToGoogleTasks(id: Long, children: List<Long>, filter: CaldavFilter) {
        if (!filter.isGoogleTasks) {
            // TODO: make sure its the same account
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
            preferences.addTasksToTop()
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