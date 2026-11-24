package org.tasks.googleapis

import kotlinx.io.IOException
import kotlinx.serialization.Serializable

@Serializable
data class GtasksTask(
    val id: String? = null,
    val etag: String? = null,
    val title: String? = null,
    val notes: String? = null,
    val status: String? = null,
    val due: String? = null,
    val completed: String? = null,
    val deleted: Boolean? = null,
    val hidden: Boolean? = null,
    val parent: String? = null,
    val position: String? = null,
    val updated: String? = null,
)

@Serializable
data class GtasksTaskList(
    val id: String? = null,
    val etag: String? = null,
    val title: String? = null,
    val updated: String? = null,
)

@Serializable
data class GtasksTasks(
    val items: List<GtasksTask>? = null,
    val nextPageToken: String? = null,
)

@Serializable
data class GtasksTaskLists(
    val items: List<GtasksTaskList>? = null,
    val nextPageToken: String? = null,
)

class GoogleTasksException(
    val statusCode: Int,
    message: String?,
    val errorMessage: String?,
    cause: Throwable? = null,
) : IOException(message, cause)

class HttpNotFoundException(message: String?) : IOException(message)

interface GoogleTasksApi : GoogleTaskListClient {
    suspend fun allGtaskLists(pageToken: String?): GtasksTaskLists?

    suspend fun getAllGtasksFromListId(
        listId: String?,
        lastSyncDate: Long,
        pageToken: String?,
    ): GtasksTasks?

    suspend fun getAllPositions(listId: String?, pageToken: String?): GtasksTasks?

    suspend fun createGtask(
        listId: String?,
        task: GtasksTask,
        parent: String?,
        previous: String?,
    ): GtasksTask?

    suspend fun updateGtask(listId: String?, task: GtasksTask): GtasksTask?

    suspend fun moveGtask(
        listId: String?,
        taskId: String?,
        parentId: String?,
        previousId: String?,
    ): GtasksTask?

    suspend fun deleteGtask(listId: String?, taskId: String?)
}
