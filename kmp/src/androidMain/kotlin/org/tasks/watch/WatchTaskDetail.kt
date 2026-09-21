package org.tasks.watch

data class WatchTaskDetail(
    val title: String,
    val completed: Boolean,
    val priority: Int,
    val repeating: Boolean,
    val description: String,
)
