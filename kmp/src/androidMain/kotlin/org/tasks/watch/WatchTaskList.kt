package org.tasks.watch

data class WatchTaskList(
    val totalItems: Int,
    val items: List<WatchUiItem>,
)
