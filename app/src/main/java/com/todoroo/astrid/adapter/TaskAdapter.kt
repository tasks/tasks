/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter

import com.todoroo.astrid.core.SortHelper.*
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.HIDE_UNTIL_SPECIFIC_DAY
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.data.CaldavDao
import org.tasks.data.CaldavTask
import org.tasks.data.GoogleTaskDao
import org.tasks.data.TaskContainer
import org.tasks.date.DateTimeUtils.toAppleEpoch
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.tasklist.SectionedDataSource.Companion.HEADER_COMPLETED
import org.tasks.time.DateTimeUtils.millisOfDay

open class TaskAdapter(
        private val newTasksOnTop: Boolean,
        private val googleTaskDao: GoogleTaskDao,
        private val caldavDao: CaldavDao,
        private val taskDao: TaskDao,
        private val localBroadcastManager: LocalBroadcastManager) {

    private val selected = HashSet<Long>()
    private val collapsed = mutableSetOf(HEADER_COMPLETED)
    private lateinit var dataSource: TaskAdapterDataSource

    val count: Int
        get() = dataSource.getTaskCount()

    fun setDataSource(dataSource: TaskAdapterDataSource) {
        this.dataSource = dataSource
    }

    val numSelected: Int
        get() = selected.size

    fun getSelected(): ArrayList<Long> = ArrayList(selected)

    fun setSelected(ids: Collection<Long>) {
        clearSelections()
        selected.addAll(ids)
    }

    fun clearSelections() = selected.clear()

    fun getCollapsed() = HashSet(collapsed)

    fun setCollapsed(groups: LongArray?) {
        clearCollapsed()
        groups?.toList()?.let(collapsed::addAll)
    }

    fun clearCollapsed() = collapsed.retainAll(listOf(HEADER_COMPLETED))

    open fun getIndent(task: TaskContainer): Int = task.getIndent()

    open fun canMove(source: TaskContainer, from: Int, target: TaskContainer, to: Int): Boolean {
        if (target.isGoogleTask) {
            return if (!source.hasChildren() || to <= 0 || to >= count - 1) {
                true
            } else if (from < to) {
                when {
                    target.hasChildren() -> false
                    target.hasParent() -> !getTask(to + 1).hasParent()
                    else -> true
                }
            } else {
                when {
                    target.hasChildren() -> true
                    target.hasParent() -> target.parent == source.id && target.secondarySort == 0L
                    else -> true
                }
            }
        } else {
            return !taskIsChild(source, to)
        }
    }

    open fun maxIndent(previousPosition: Int, task: TaskContainer): Int {
        val previous = getTask(previousPosition)
        return if (previous.isGoogleTask) {
            if (task.hasChildren()) 0 else 1
        } else {
            previous.indent + 1
        }
    }

    fun minIndent(nextPosition: Int, task: TaskContainer): Int {
        (nextPosition until count).forEach {
            if (isHeader(it)) {
                return 0
            }
            val next = getTask(it)
            if (next.isGoogleTask) {
                return if (task.hasChildren() || !next.hasParent()) 0 else 1
            }
            if (!taskIsChild(task, it)) {
                return next.indent
            }
        }
        return 0
    }

    fun isSelected(task: TaskContainer): Boolean = selected.contains(task.id)

    fun toggleSelection(task: TaskContainer) {
        val id = task.id
        if (selected.contains(id)) {
            selected.remove(id)
        } else {
            selected.add(id)
        }
    }

    fun toggleCollapsed(group: Long) {
        if (collapsed.contains(group)) {
            collapsed.remove(group)
        } else {
            collapsed.add(group)
        }
    }

    open fun supportsAstridSorting(): Boolean = false

    open suspend fun moved(from: Int, to: Int, indent: Int) {
        val task = getTask(from)
        val newParent = findParent(indent, to)
        if ((newParent?.id ?: 0) == task.parent) {
            if (indent == 0) {
                changeSortGroup(task, if (from < to) to - 1 else to)
            }
            return
        } else if (newParent != null) {
            if (task.caldav != newParent.caldav) {
                caldavDao.markDeleted(listOf(task.id))
                task.caldavTask = null
            }
        }
        when {
            newParent == null -> {
                moveToTopLevel(task)
                changeSortGroup(task, if (from < to) to - 1 else to)
            }
            newParent.isGoogleTask -> changeGoogleTaskParent(task, newParent)
            newParent.isCaldavTask -> changeCaldavParent(task, newParent)
        }
    }

    fun isHeader(position: Int): Boolean = dataSource.isHeader(position)

    fun getTask(position: Int): TaskContainer = dataSource.getItem(position)!!

    fun getItemUuid(position: Int): String = getTask(position).uuid

    open suspend fun onCompletedTask(task: TaskContainer, newState: Boolean) {}

    open suspend fun onTaskCreated(uuid: String) {}

    open suspend fun onTaskDeleted(task: Task) {}

    open fun supportsHiddenTasks(): Boolean = true

    private fun taskIsChild(source: TaskContainer, destinationIndex: Int): Boolean {
        (destinationIndex downTo 0).forEach {
            if (isHeader(it)) {
                return false
            }
            when (getTask(it).parent) {
                0L -> return false
                source.parent -> return false
                source.id -> return true
            }
        }
        return false
    }

    internal fun findParent(indent: Int, to: Int): TaskContainer? {
        if (indent == 0 || to == 0) {
            return null
        }
        for (i in to - 1 downTo 0) {
            val previous = getTask(i)
            if (indent > previous.getIndent()) {
                return previous
            }
        }
        return null
    }

    private suspend fun changeSortGroup(task: TaskContainer, pos: Int) {
        when(dataSource.sortMode) {
            SORT_IMPORTANCE -> {
                val newPriority = dataSource.nearestHeader(if (pos == 0) 1 else pos).toInt()
                if (newPriority != task.priority) {
                    val t = task.getTask()
                    t.priority = newPriority
                    taskDao.save(t)
                }
            }
            SORT_DUE -> applyDueDate(task.task, dataSource.nearestHeader(if (pos == 0) 1 else pos))
            SORT_START -> applyStartDate(task.task, dataSource.nearestHeader(if (pos == 0) 1 else pos))
        }
    }

    private suspend fun applyDueDate(task: Task, date: Long) {
        val original = task.dueDate
        task.setDueDateAdjustingHideUntil(when {
            date == 0L -> 0L
            task.hasDueTime() -> date.toDateTime().withMillisOfDay(original.millisOfDay()).millis
            else -> Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, date)
        })
        if (original != task.dueDate) {
            taskDao.save(task)
        }
    }

    private suspend fun applyStartDate(task: Task, date: Long) {
        val original = task.hideUntil
        task.hideUntil = when {
            date == 0L -> 0L
            task.hasStartDate() -> date.toDateTime().withMillisOfDay(original.millisOfDay()).millis
            else -> task.createHideUntil(HIDE_UNTIL_SPECIFIC_DAY, date)
        }
        if (original != task.hideUntil) {
            taskDao.save(task)
        }
    }

    private suspend fun moveToTopLevel(task: TaskContainer) {
        when {
            task.isGoogleTask -> changeGoogleTaskParent(task, null)
            task.isCaldavTask -> changeCaldavParent(task, null)
        }
    }

    private suspend fun changeGoogleTaskParent(task: TaskContainer, newParent: TaskContainer?) {
        val list = newParent?.caldav ?: task.caldav!!
        if (newParent == null || task.caldav == newParent.caldav) {
            googleTaskDao.move(
                task.task,
                list,
                newParent?.id ?: 0,
                if (newTasksOnTop) 0 else googleTaskDao.getBottom(list, newParent?.id ?: 0)
            )
        } else {
            task.parent = newParent.id
            task.caldavTask = CaldavTask(task.id, list)
            googleTaskDao.insertAndShift(task.task, task.caldavTask, newTasksOnTop)
        }
        taskDao.touch(task.id)
        if (BuildConfig.DEBUG) {
            googleTaskDao.validateSorting(list)
        }
    }

    private suspend fun changeCaldavParent(task: TaskContainer, newParent: TaskContainer?) {
        val list = newParent?.caldav ?: task.caldav!!
        val caldavTask = task.getCaldavTask() ?: CaldavTask(
            task.id,
            list,
        )
        val newParentId = newParent?.id ?: 0
        if (newParentId == 0L) {
            caldavTask.remoteParent = ""
        } else {
            val parentTask = caldavDao.getTask(newParentId) ?: return
            caldavTask.calendar = list
            caldavTask.remoteParent = parentTask.remoteId
        }
        task.task.order = if (newTasksOnTop) {
            caldavDao.findFirstTask(list, newParentId)
                    ?.takeIf { task.creationDate.toAppleEpoch() >= it}
                    ?.minus(1)
        } else {
            caldavDao.findLastTask(list, newParentId)
                    ?.takeIf { task.creationDate.toAppleEpoch() <= it }
                    ?.plus(1)
        }
        if (caldavTask.id == 0L) {
            val newTask = CaldavTask(task.id, list)
            newTask.remoteParent = caldavTask.remoteParent
            caldavTask.id = caldavDao.insert(newTask)
            task.caldavTask = caldavTask
        } else {
            caldavDao.update(caldavTask)
        }
        taskDao.setOrder(task.id, task.task.order)
        taskDao.setParent(newParentId, listOf(task.id))
        taskDao.touch(task.id)
        localBroadcastManager.broadcastRefresh()
    }
}