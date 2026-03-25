package org.tasks.watch

sealed class WatchUiItem {
    data class Header(
        val id: Long,
        val title: String,
        val collapsed: Boolean,
    ) : WatchUiItem()

    data class Task(
        val id: Long,
        val title: String?,
        val priority: Int,
        val completed: Boolean,
        val hidden: Boolean,
        val indent: Int,
        val collapsed: Boolean,
        val numSubtasks: Int,
        val timestamp: String?,
        val repeating: Boolean,
    ) : WatchUiItem()
}
