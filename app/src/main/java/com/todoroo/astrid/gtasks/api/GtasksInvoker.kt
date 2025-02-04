package com.todoroo.astrid.gtasks.api

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.tasks.Tasks
import com.google.api.services.tasks.model.Task
import com.google.api.services.tasks.model.TaskList
import com.google.api.services.tasks.model.TaskLists
import org.tasks.googleapis.BaseInvoker
import java.io.IOException

/**
 * Wrapper around the official Google Tasks API to simplify common operations. In the case of an
 * exception, each request is tried twice in case of a timeout.
 *
 * @author Sam Bosley
 */
class GtasksInvoker(
        credentials: HttpCredentialsAdapter,
) : BaseInvoker(credentials) {
    private val service =
            Tasks.Builder(NetHttpTransport(), GsonFactory(), credentials)
                    .setApplicationName(APP_NAME)
                    .build()

    @Throws(IOException::class)
    suspend fun allGtaskLists(pageToken: String?): TaskLists? =
            execute(service!!.tasklists().list().setMaxResults(100).setPageToken(pageToken))

    @Throws(IOException::class)
    suspend fun getAllGtasksFromListId(
            listId: String?, lastSyncDate: Long, pageToken: String?): com.google.api.services.tasks.model.Tasks? =
            execute(
                    service!!
                            .tasks()
                            .list(listId)
                            .setMaxResults(100)
                            .setShowDeleted(true)
                            .setShowHidden(true)
                            .setPageToken(pageToken)
                            .setUpdatedMin(
                                    GtasksApiUtilities.unixTimeToGtasksCompletionTime(lastSyncDate).toStringRfc3339()))

    @Throws(IOException::class)
    suspend fun getAllPositions(
            listId: String?, pageToken: String?): com.google.api.services.tasks.model.Tasks? =
            execute(
                    service!!
                            .tasks()
                            .list(listId)
                            .setMaxResults(100)
                            .setShowDeleted(false)
                            .setShowHidden(false)
                            .setPageToken(pageToken)
                            .setFields("items(id,parent,position),nextPageToken"))

    @Throws(IOException::class)
    suspend fun createGtask(
            listId: String?, task: Task?, parent: String?, previous: String?): Task? =
            execute(service!!.tasks().insert(listId, task).setParent(parent).setPrevious(previous))

    @Throws(IOException::class)
    suspend fun updateGtask(listId: String?, task: Task) =
            execute(service!!.tasks().update(listId, task.id, task))

    @Throws(IOException::class)
    suspend fun moveGtask(
            listId: String?, taskId: String?, parentId: String?, previousId: String?): Task? =
            execute(
                    service!!
                            .tasks()
                            .move(listId, taskId)
                            .setParent(parentId)
                            .setPrevious(previousId))

    @Throws(IOException::class)
    suspend fun deleteGtaskList(listId: String?) {
        try {
            execute(service!!.tasklists().delete(listId))
        } catch (ignored: HttpNotFoundException) {
        }
    }

    @Throws(IOException::class)
    suspend fun renameGtaskList(listId: String?, title: String?): TaskList? =
            execute(service!!.tasklists().patch(listId, TaskList().setTitle(title)))

    @Throws(IOException::class)
    suspend fun createGtaskList(title: String?): TaskList? =
            execute(service!!.tasklists().insert(TaskList().setTitle(title)))

    @Throws(IOException::class)
    suspend fun deleteGtask(listId: String?, taskId: String?) {
        try {
            execute(service!!.tasks().delete(listId, taskId))
        } catch (ignored: HttpNotFoundException) {
        }
    }
}