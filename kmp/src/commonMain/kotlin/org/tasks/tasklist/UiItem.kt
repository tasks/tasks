package org.tasks.tasklist

import org.tasks.data.TaskContainer

sealed class UiItem {
    data class Header(val value: Long): UiItem()
    data class Task(val task: TaskContainer): UiItem()
}
