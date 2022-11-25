package com.todoroo.astrid.service

import android.content.Context
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.data.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.caldav.VtodoCache
import org.tasks.data.*
import org.tasks.db.DbUtils.dbchunk
import org.tasks.preferences.Preferences
import org.tasks.sync.SyncAdapters
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
                return GtasksFilter(googleTaskListDao.getByRemoteId(googleTaskLists[0]))
            }
        } else if (googleTaskLists.isEmpty()) {
            if (caldavCalendars.size == 1) {
                return CaldavFilter(caldavDao.getCalendar(caldavCalendars[0]))
            }
        }
        return null
    }

    suspend fun move(ids: List<Long>, selectedList: Filter) {
        val tasks = ids
            .dbchunk()
            .flatMap {
                it.minus(googleTaskDao.getChildren(it).toSet())
                    .minus(taskDao.getChildren(it).toSet())
            }
            .let { taskDao.fetch(it) }
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

    private suspend fun moveGoogleTask(task: Task, googleTask: GoogleTask, selected: Filter) {
        if (selected is GtasksFilter && googleTask.listId == selected.remoteId) {
            return
        }
        val id = googleTask.task
        val children = googleTaskDao.getChildren(id)
        val childIds = children.map(GoogleTask::task)
        googleTaskDao.markDeleted(id, DateUtilities.now())
        when(selected) {
            is GtasksFilter -> {
                val listId = selected.remoteId
                googleTaskDao.insertAndShift(GoogleTask(id, listId), preferences.addTasksToTop())
                children.takeIf { it.isNotEmpty() }
                        ?.map {
                            val newChild = GoogleTask(it.task, listId)
                            newChild.order = it.order
                            newChild.parent = id
                            newChild
                        }
                        ?.let { googleTaskDao.insert(it) }
            }
            is CaldavFilter -> {
                val listId = selected.uuid
                val newParent = CaldavTask(id, listId)
                caldavDao.insert(task, newParent, preferences.addTasksToTop())
                childIds.map {
                    val newChild = CaldavTask(it, listId)
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
        caldavDao.markDeleted(toDelete, DateUtilities.now())
        when (selected) {
            is CaldavFilter -> {
                val from = caldavDao.getCalendar(caldavTask.calendar!!)
                val id1 = caldavTask.task
                val listId = selected.uuid
                val newParent = CaldavTask(id1, listId, caldavTask.remoteId, caldavTask.`object`)
                vtodoCache.move(from!!, selected.calendar, caldavTask)
                caldavDao.insert(task, newParent, preferences.addTasksToTop())
                children.takeIf { it.isNotEmpty() }
                        ?.map {
                            val newChild = CaldavTask(it.task, listId, it.remoteId, it.`object`)
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
                val root = CaldavTask(id, listId)
                val children = taskDao.getChildren(id).mapNotNull { taskDao.fetch(it) }
                for (child in children) {
                    val newTask = CaldavTask(child.id, listId)
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
        taskDao.setParent(0, children)
        val listId = filter.remoteId
        googleTaskDao.insertAndShift(GoogleTask(id, listId), preferences.addTasksToTop())
        children.takeIf { it.isNotEmpty() }
                ?.mapIndexed { index, task ->
                    val newChild = GoogleTask(task, listId)
                    newChild.order = index.toLong()
                    newChild.parent = id
                    newChild
                }
                ?.let { googleTaskDao.insert(it) }
    }
}