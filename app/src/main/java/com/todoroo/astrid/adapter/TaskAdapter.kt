/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter

import com.todoroo.astrid.core.SortHelper.SORT_DUE
import com.todoroo.astrid.core.SortHelper.SORT_IMPORTANCE
import com.todoroo.astrid.core.SortHelper.SORT_LIST
import com.todoroo.astrid.core.SortHelper.SORT_MANUAL
import com.todoroo.astrid.core.SortHelper.SORT_START
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.HIDE_UNTIL_SPECIFIC_DAY
import com.todoroo.astrid.service.TaskMover
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
        private val localBroadcastManager: LocalBroadcastManager,
        private val taskMover: TaskMover,
) {
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

    open fun getIndent(task: TaskContainer): Int = task.indent

    open fun canMove(source: TaskContainer, from: Int, target: TaskContainer, to: Int): Boolean {
        if (target.isSingleLevelSubtask) {
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
        return if (previous.isSingleLevelSubtask) {
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
            if (next.isSingleLevelSubtask) {
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
            } else if (dataSource.subtaskSortMode == SORT_MANUAL) {
                if (task.isGoogleTask) {
                    moveGoogleTask(from, to, indent)
                } else {
                    moveCaldavTask(from, to, indent)
                }
            }
            return
        } else if (newParent != null) {
            if (task.caldav != newParent.caldav) {
                caldavDao.markDeleted(listOf(task.id))
            }
        }
        when {
            newParent == null -> {
                moveToTopLevel(task)
                changeSortGroup(task, if (from < to) to - 1 else to)
            }
            newParent.isGoogleTask -> changeGoogleTaskParent(task, newParent)
            newParent.isCaldavTask() -> changeCaldavParent(task, newParent)
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

    private fun findParent(indent: Int, to: Int): TaskContainer? {
        if (indent == 0 || to == 0) {
            return null
        }
        for (i in to - 1 downTo 0) {
            val previous = getTask(i)
            if (indent > previous.indent) {
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
                    val t = task.task
                    t.priority = newPriority
                    taskDao.save(t)
                }
            }
            SORT_LIST -> taskMover.move(task.id, dataSource.nearestHeader(if (pos == 0) 1 else pos))
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
            task.isCaldavTask() -> changeCaldavParent(task, null)
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
            googleTaskDao.insertAndShift(
                task = task.task,
                caldavTask = CaldavTask(
                    task = task.id,
                    calendar = list,
                    remoteId = null
                ),
                top = newTasksOnTop
            )
        }
        taskDao.touch(task.id)
        if (BuildConfig.DEBUG) {
            googleTaskDao.validateSorting(list)
        }
    }

    private suspend fun changeCaldavParent(task: TaskContainer, newParent: TaskContainer?) {
        val list = newParent?.caldav ?: task.caldav!!
        val caldavTask = task.caldavTask ?: CaldavTask(
            task = task.id,
            calendar = list,
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
            val newTask = CaldavTask(
                task = task.id,
                calendar = list,
            )
            newTask.remoteParent = caldavTask.remoteParent
            caldavDao.insert(newTask)
        } else {
            caldavDao.update(caldavTask)
        }
        taskDao.setOrder(task.id, task.task.order)
        taskDao.setParent(newParentId, listOf(task.id))
        taskDao.touch(task.id)
        localBroadcastManager.broadcastRefresh()
    }

    protected suspend fun moveGoogleTask(from: Int, to: Int, indent: Int) {
        val task = getTask(from)
        val googleTask = task.caldavTask ?: return
        val list = googleTask.calendar ?: return
        val previous = if (to > 0) getTask(to - 1) else null
        if (previous == null) {
            googleTaskDao.move(
                task = task.task,
                list = list,
                newParent = 0,
                newPosition = 0,
            )
        } else if (to == count || to <= from) {
            when {
                indent == 0 ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = 0,
                        newPosition = previous.primarySort + if (to == count) 0 else 1,
                    )
                previous.hasParent() && previous.parent == task.parent ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = previous.parent,
                        newPosition = previous.secondarySort + if (to == count) 0 else 1,
                    )
                previous.hasParent() ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = previous.parent,
                        newPosition = previous.secondarySort + 1,
                    )
                else ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = previous.id,
                        newPosition = 0,
                    )
            }
        } else {
            when {
                indent == 0 ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = 0,
                        newPosition = previous.primarySort + if (task.hasParent()) 1 else 0,
                    )
                previous.hasParent() && previous.parent == task.parent ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = previous.parent,
                        newPosition = previous.secondarySort,
                    )
                previous.hasParent() ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = previous.parent,
                        newPosition = previous.secondarySort + 1,
                    )
                else ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = previous.id,
                        newPosition = 0,
                    )
            }
        }
        taskDao.touch(task.id)
        localBroadcastManager.broadcastRefresh()
        if (BuildConfig.DEBUG) {
            googleTaskDao.validateSorting(task.caldav!!)
        }
    }

    protected suspend fun moveCaldavTask(from: Int, to: Int, indent: Int) {
        val task = getTask(from)
        val oldParent = task.parent
        val newParent = changeCaldavParent(task, indent, to)

        if (oldParent == newParent && from == to) {
            return
        }

        val previous = if (to > 0) getTask(to - 1) else null
        val next = if (to < count) getTask(to) else null

        val newPosition = when {
            previous == null -> next!!.caldavSortOrder - 1
            indent > previous.indent && next?.indent == indent -> next.caldavSortOrder - 1
            indent > previous.indent -> null
            indent == previous.indent -> previous.caldavSortOrder + 1
            else -> getTask((to - 1 downTo 0).find { getTask(it).indent == indent }!!).caldavSortOrder + 1
        }
        caldavDao.move(task, oldParent, newParent, newPosition)
        taskDao.touch(task.id)
        localBroadcastManager.broadcastRefresh()
    }

    private suspend fun changeCaldavParent(task: TaskContainer, indent: Int, to: Int): Long {
        val newParent = findParent(indent, to)?.id ?: 0
        if (task.parent != newParent) {
            changeCaldavParent(task, newParent)
        }
        return newParent
    }

    private suspend fun changeCaldavParent(task: TaskContainer, newParent: Long) {
        val caldavTask = task.caldavTask ?: return
        if (newParent == 0L) {
            caldavTask.remoteParent = ""
            task.parent = 0
        } else {
            val parentTask = caldavDao.getTask(newParent) ?: return
            caldavTask.remoteParent = parentTask.remoteId
            task.parent = newParent
        }
        caldavDao.update(caldavTask.id, caldavTask.remoteParent)
        taskDao.save(task.task, null)
    }
}