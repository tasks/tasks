package com.todoroo.astrid.service

import android.content.Context
import org.tasks.filters.CaldavFilter
import org.tasks.filters.GtasksFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.caldav.VtodoCache
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.GoogleTaskListDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.db.DbUtils.dbchunk
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.getLocalList
import org.tasks.filters.Filter
import org.tasks.preferences.Preferences
import org.tasks.sync.SyncAdapters
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import javax.inject.Inject

class TaskMover @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val caldavDao: CaldavDao,
    private val googleTaskDao: GoogleTaskDao,
    private val googleTaskListDao: GoogleTaskListDao,
    private val preferences: Preferences,
    private val localBroadcastManager: LocalBroadcastManager,
    private val syncAdapters: SyncAdapters,
    private val vtodoCache: VtodoCache,
) {

    suspend fun getSingleFilter(tasks: List<Long>): Filter? {
        val caldavCalendars = caldavDao.getCalendars(tasks)
        val googleTaskLists = googleTaskDao.getLists(tasks)
        if (caldavCalendars.isEmpty()) {
            if (googleTaskLists.size == 1) {
                return googleTaskListDao.getByRemoteId(googleTaskLists[0])?.let { GtasksFilter(it) }
            }
        } else if (googleTaskLists.isEmpty()) {
            if (caldavCalendars.size == 1) {
                return caldavDao.getCalendar(caldavCalendars[0])?.let { CaldavFilter(it) }
            }
        }
        return null
    }

    suspend fun move(task: Long, list: Long) {
        val calendar = caldavDao.getCalendarById(list) ?: return
        val account = calendar.account?.let { caldavDao.getAccountByUuid(it) } ?: return
        move(
            ids = listOf(task),
            selectedList = if (account.accountType == CaldavAccount.TYPE_GOOGLE_TASKS)
                GtasksFilter(calendar)
            else
                CaldavFilter(calendar)
        )
    }

    suspend fun move(ids: List<Long>, selectedList: Filter) {
        val tasks = ids
            .dbchunk()
            .flatMap { taskDao.getChildren(it) }
            .let { taskDao.fetch(ids.minus(it.toSet())) }
            .filterNot { it.readOnly }
        val taskIds = tasks.map { it.id }
        taskDao.setParent(0, ids.intersect(taskIds.toSet()).toList())
        tasks.forEach { performMove(it, selectedList) }
        if (selectedList is CaldavFilter) {
            caldavDao.updateParents(selectedList.uuid)
        }
        taskIds.dbchunk().forEach {
            taskDao.touch(it)
        }
        localBroadcastManager.broadcastRefresh()
        syncAdapters.sync()
    }

    suspend fun migrateLocalTasks() {
        val list = caldavDao.getLocalList(context)
        move(taskDao.getLocalTasks(), CaldavFilter(list))
    }

    private suspend fun performMove(task: Task, selectedList: Filter) {
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

    private suspend fun moveGoogleTask(task: Task, googleTask: CaldavTask, selected: Filter) {
        if (selected is GtasksFilter && googleTask.calendar == selected.remoteId) {
            return
        }
        val id = task.id
        val children = taskDao.getChildren(id)
        caldavDao.markDeleted(children + id, currentTimeMillis())
        when(selected) {
            is GtasksFilter -> {
                val listId = selected.remoteId
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
            is CaldavFilter -> {
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
            else -> require(!BuildConfig.DEBUG)
        }
    }

    private suspend fun moveCaldavTask(task: Task, caldavTask: CaldavTask, selected: Filter) {
        if (selected is CaldavFilter
                && caldavTask.calendar == selected.uuid) {
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
        when (selected) {
            is CaldavFilter -> {
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
            is GtasksFilter -> moveToGoogleTasks(id, childIds, selected)
            else -> require(!BuildConfig.DEBUG)
        }
    }

    private suspend fun moveLocalTask(task: Task, selected: Filter) {
        when (selected) {
            is GtasksFilter -> moveToGoogleTasks(task.id, taskDao.getChildren(task.id), selected)
            is CaldavFilter -> {
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
            else -> require(!BuildConfig.DEBUG)
        }
    }

    private suspend fun moveToGoogleTasks(id: Long, children: List<Long>, filter: GtasksFilter) {
        val task = taskDao.fetch(id) ?: return
        taskDao.setParent(id, children)
        val listId = filter.remoteId
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