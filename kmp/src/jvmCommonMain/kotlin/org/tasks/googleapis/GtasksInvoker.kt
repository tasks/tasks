package org.tasks.googleapis

import co.touchlab.kermit.Logger
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.tasks.Tasks
import com.google.api.services.tasks.model.Task
import com.google.api.services.tasks.model.TaskList
import com.google.api.services.tasks.model.TaskLists
import org.tasks.TasksBuildConfig
import java.io.IOException

class GtasksInvoker(
    credentials: CredentialsAdapter,
) : BaseInvoker(credentials), GoogleTasksApi {
    private val service =
        Tasks.Builder(NetHttpTransport(), GsonFactory(), credentials)
            .setApplicationName(APP_NAME)
            .build()

    @Throws(IOException::class)
    override suspend fun allGtaskLists(pageToken: String?): GtasksTaskLists? = gtasks {
        execute(service.tasklists().list().setMaxResults(100).setPageToken(pageToken))
            ?.toGtasks()
    }

    @Throws(IOException::class)
    override suspend fun getAllGtasksFromListId(
        listId: String?, lastSyncDate: Long, pageToken: String?,
    ): GtasksTasks? = gtasks {
        execute(
            service
                .tasks()
                .list(listId)
                .setMaxResults(100)
                .setShowDeleted(true)
                .setShowHidden(true)
                .setPageToken(pageToken)
                .setUpdatedMin(GtasksDates.unixTimeToGtasksCompletionTime(lastSyncDate))
        )?.toGtasks()
    }

    @Throws(IOException::class)
    override suspend fun getAllPositions(
        listId: String?,
        pageToken: String?,
    ): GtasksTasks? = gtasks {
        execute(
            service
                .tasks()
                .list(listId)
                .setMaxResults(100)
                .setShowDeleted(false)
                .setShowHidden(false)
                .setPageToken(pageToken)
                .setFields("items(id,parent,position),nextPageToken")
        )?.toGtasks()
    }

    @Throws(IOException::class)
    override suspend fun createGtask(
        listId: String?,
        task: GtasksTask,
        parent: String?,
        previous: String?,
    ): GtasksTask? = gtasks {
        val javaTask = task.toJava()
        Logger.d(TAG) { "createGtask(listId=$listId, task=${javaTask.describe()}, parent=$parent, previous=$previous)" }
        execute(service.tasks().insert(listId, javaTask).setParent(parent).setPrevious(previous))
            ?.toGtasks()
    }

    @Throws(IOException::class)
    override suspend fun updateGtask(listId: String?, task: GtasksTask): GtasksTask? = gtasks {
        val javaTask = task.toJava()
        Logger.d(TAG) { "updateGtask(listId=$listId, task=${javaTask.describe()})" }
        execute(service.tasks().update(listId, task.id, javaTask))?.toGtasks()
    }

    @Throws(IOException::class)
    override suspend fun moveGtask(
        listId: String?,
        taskId: String?,
        parentId: String?,
        previousId: String?,
    ): GtasksTask? = gtasks {
        Logger.d(TAG) { "moveGtask(listId=$listId, taskId=$taskId, parentId=$parentId, previousId=$previousId)" }
        execute(
            service
                .tasks()
                .move(listId, taskId)
                .setParent(parentId)
                .setPrevious(previousId)
        )?.toGtasks()
    }

    @Throws(IOException::class)
    override suspend fun deleteGtaskList(listId: String) = gtasks {
        try {
            execute(service.tasklists().delete(listId))
        } catch (_: HttpNotFoundException) {
        }
        Unit
    }

    @Throws(IOException::class)
    override suspend fun renameGtaskList(listId: String, title: String) = gtasks {
        execute(service.tasklists().patch(listId, TaskList().setTitle(title)))
        Unit
    }

    @Throws(IOException::class)
    override suspend fun createGtaskList(title: String): GoogleTaskList = gtasks {
        execute(service.tasklists().insert(TaskList().setTitle(title)))
            ?.let { GoogleTaskList(id = it.id, title = it.title) }
            ?: throw IllegalStateException("Google Tasks did not return a list")
    }

    @Throws(IOException::class)
    override suspend fun deleteGtask(listId: String?, taskId: String?) = gtasks {
        Logger.d(TAG) { "deleteGtask(listId=$listId, taskId=$taskId)" }
        try {
            execute(service.tasks().delete(listId, taskId))
        } catch (_: HttpNotFoundException) {
        }
        Unit
    }

    private suspend fun <T> gtasks(block: suspend () -> T): T =
        try {
            block()
        } catch (e: GoogleJsonResponseException) {
            throw GoogleTasksException(
                statusCode = e.statusCode,
                message = e.message,
                errorMessage = e.details?.message,
                cause = e,
            )
        }

    private fun Task?.describe(): String = when {
        this == null -> "null"
        TasksBuildConfig.DEBUG -> runCatching { toPrettyString() }.getOrElse { safeFields() }
        else -> safeFields()
    }

    private fun Task.safeFields(): String =
        "Task(id=$id, parent=$parent, position=$position, status=$status, " +
                "due=$due, completed=$completed, deleted=$deleted, hidden=$hidden)"

    companion object {
        private const val TAG = "GtasksInvoker"
    }
}

internal fun Task.toGtasks() = GtasksTask(
    id = id,
    etag = etag,
    title = title,
    notes = notes,
    status = status,
    due = due,
    completed = completed,
    deleted = deleted,
    hidden = hidden,
    parent = parent,
    position = position,
    updated = updated,
)

internal fun GtasksTask.toJava(): Task = Task().also { task ->
    id?.let { task.id = it }
    etag?.let { task.etag = it }
    title?.let { task.title = it }
    notes?.let { task.notes = it }
    status?.let { task.status = it }
    due?.let { task.due = it }
    completed?.let { task.completed = it }
    deleted?.let { task.deleted = it }
    hidden?.let { task.hidden = it }
    parent?.let { task.parent = it }
    position?.let { task.position = it }
    updated?.let { task.updated = it }
}

internal fun TaskList.toGtasks() = GtasksTaskList(
    id = id,
    etag = etag,
    title = title,
    updated = updated,
)

internal fun com.google.api.services.tasks.model.Tasks.toGtasks() = GtasksTasks(
    items = items?.map { it.toGtasks() },
    nextPageToken = nextPageToken,
)

internal fun TaskLists.toGtasks() = GtasksTaskLists(
    items = items?.map { it.toGtasks() },
    nextPageToken = nextPageToken,
)
