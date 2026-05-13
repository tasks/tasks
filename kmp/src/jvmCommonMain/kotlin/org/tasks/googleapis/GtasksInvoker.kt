package org.tasks.googleapis

import co.touchlab.kermit.Logger
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.tasks.Tasks
import com.google.api.services.tasks.model.Task
import com.google.api.services.tasks.model.TaskList
import com.google.api.services.tasks.model.TaskLists
import java.io.IOException

class GtasksInvoker(
    credentials: CredentialsAdapter,
) : BaseInvoker(credentials) {
    private val service =
        Tasks.Builder(NetHttpTransport(), GsonFactory(), credentials)
            .setApplicationName(APP_NAME)
            .build()

    @Throws(IOException::class)
    suspend fun allGtaskLists(pageToken: String?): TaskLists? =
        execute(service.tasklists().list().setMaxResults(100).setPageToken(pageToken))

    @Throws(IOException::class)
    suspend fun getAllGtasksFromListId(
        listId: String?, lastSyncDate: Long, pageToken: String?,
    ): com.google.api.services.tasks.model.Tasks? =
        execute(
            service
                .tasks()
                .list(listId)
                .setMaxResults(100)
                .setShowDeleted(true)
                .setShowHidden(true)
                .setPageToken(pageToken)
                .setUpdatedMin(
                    GtasksApiUtilities.unixTimeToGtasksCompletionTime(lastSyncDate)
                        ?.toStringRfc3339()
                )
        )

    @Throws(IOException::class)
    suspend fun getAllPositions(
        listId: String?,
        pageToken: String?,
    ): com.google.api.services.tasks.model.Tasks? =
        execute(
            service
                .tasks()
                .list(listId)
                .setMaxResults(100)
                .setShowDeleted(false)
                .setShowHidden(false)
                .setPageToken(pageToken)
                .setFields("items(id,parent,position),nextPageToken")
        )

    @Throws(IOException::class)
    suspend fun createGtask(
        listId: String?,
        task: Task?,
        parent: String?,
        previous: String?,
    ): Task? {
        Logger.d(TAG) { "createGtask(listId=$listId, task=<redacted>, parent=$parent, previous=$previous)" }
        return execute(service.tasks().insert(listId, task).setParent(parent).setPrevious(previous))
    }

    @Throws(IOException::class)
    suspend fun updateGtask(listId: String?, task: Task) =
        execute(service.tasks().update(listId, task.id, task))

    @Throws(IOException::class)
    suspend fun moveGtask(
        listId: String?,
        taskId: String?,
        parentId: String?,
        previousId: String?,
    ): Task? {
        Logger.d(TAG) { "moveGtask(listId=$listId, taskId=$taskId, parentId=$parentId, previousId=$previousId)" }
        return execute(
            service
                .tasks()
                .move(listId, taskId)
                .setParent(parentId)
                .setPrevious(previousId)
        )
    }

    @Throws(IOException::class)
    suspend fun deleteGtaskList(listId: String?) {
        try {
            execute(service.tasklists().delete(listId))
        } catch (_: HttpNotFoundException) {
        }
    }

    @Throws(IOException::class)
    suspend fun renameGtaskList(listId: String?, title: String?): TaskList? =
        execute(service.tasklists().patch(listId, TaskList().setTitle(title)))

    @Throws(IOException::class)
    suspend fun createGtaskList(title: String?): TaskList? =
        execute(service.tasklists().insert(TaskList().setTitle(title)))

    @Throws(IOException::class)
    suspend fun deleteGtask(listId: String?, taskId: String?) {
        Logger.d(TAG) { "deleteGtask(listId=$listId, taskId=$taskId)" }
        try {
            execute(service.tasks().delete(listId, taskId))
        } catch (_: HttpNotFoundException) {
        }
    }

    companion object {
        private const val TAG = "GtasksInvoker"
    }
}
