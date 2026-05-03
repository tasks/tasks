package org.tasks.watch

interface WatchService {
    suspend fun getTasks(
        filterPreference: String?,
        position: Int,
        limit: Int,
        showHidden: Boolean,
        showCompleted: Boolean,
        sortMode: Int?,
        groupMode: Int?,
        collapsed: Set<Long>,
    ): WatchTaskList

    suspend fun completeTask(taskId: Long, completed: Boolean, source: String)

    suspend fun getLists(position: Int, limit: Int): WatchListItems

    suspend fun getTask(taskId: Long): WatchTaskDetail

    suspend fun saveTask(
        taskId: Long,
        title: String,
        completed: Boolean,
        filterPreference: String?,
        source: String,
    ): Long

    suspend fun getTaskCount(
        filterPreference: String?,
        showHidden: Boolean,
        showCompleted: Boolean,
    ): WatchTaskCount
}
