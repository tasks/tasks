/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter

import com.todoroo.astrid.data.Task
import org.tasks.data.TaskContainer
import java.util.*
import kotlin.collections.HashSet

open class TaskAdapter {
    private val selected = HashSet<Long>()
    private val collapsed = HashSet<Long>()
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

    fun getCollapsed(): MutableSet<Long> = HashSet(collapsed)

    fun setCollapsed(groups: LongArray?) {
        clearCollapsed()
        groups?.toList()?.let(collapsed::addAll)
    }

    fun clearCollapsed() = collapsed.clear()

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

    open fun supportsParentingOrManualSort(): Boolean = false

    open fun supportsManualSorting(): Boolean = false

    open fun moved(from: Int, to: Int, indent: Int) {}

    fun isHeader(position: Int): Boolean = dataSource.isHeader(position)

    fun getTask(position: Int): TaskContainer = dataSource.getItem(position)

    fun getItemUuid(position: Int): String = getTask(position).uuid

    open fun onCompletedTask(task: TaskContainer, newState: Boolean) {}

    open fun onTaskCreated(uuid: String) {}

    open fun onTaskDeleted(task: Task) {}

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
}