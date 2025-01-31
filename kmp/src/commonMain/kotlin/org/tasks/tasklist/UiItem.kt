package org.tasks.tasklist

import org.tasks.data.TaskContainer

sealed class UiItem {
    data class Header(
        val value: Long,
        val collapsed: Boolean,
    ) : UiItem()

    data class Task(val task: TaskContainer) : UiItem()

    val key: String
        get() = when (this) {
            is Header -> "header_$value"
            is Task -> task.id.toString()
        }
}
