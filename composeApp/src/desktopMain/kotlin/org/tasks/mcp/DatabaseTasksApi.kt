package org.tasks.mcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.api.AccountQuery
import org.tasks.api.AccountRow
import org.tasks.api.ApiPage
import org.tasks.api.ApiQueryEngine
import org.tasks.api.ApiWriter
import org.tasks.api.Completion
import org.tasks.api.Creation
import org.tasks.api.Deletion
import org.tasks.api.ListQuery
import org.tasks.api.ListRow
import org.tasks.api.ListWrite
import org.tasks.api.PlaceQuery
import org.tasks.api.PlaceRow
import org.tasks.api.PlaceWrite
import org.tasks.api.ReminderEdit
import org.tasks.api.ReminderQuery
import org.tasks.api.ReminderRow
import org.tasks.api.ReminderWrite
import org.tasks.api.Revision
import org.tasks.api.TagChange
import org.tasks.api.TagQuery
import org.tasks.api.TagRow
import org.tasks.api.TagWrite
import org.tasks.api.TaskQuery
import org.tasks.api.TaskRow
import org.tasks.api.TaskUpdate
import org.tasks.api.TaskWrite
import org.tasks.api.TasksContract
import org.tasks.api.changeList
import org.tasks.api.changePlace
import org.tasks.api.changeTag
import org.tasks.api.completeTasks
import org.tasks.api.countTasks
import org.tasks.api.createList
import org.tasks.api.Created
import org.tasks.api.createPlace
import org.tasks.api.createTag
import org.tasks.api.createTasks
import org.tasks.api.deleteList
import org.tasks.api.deletePlace
import org.tasks.api.deleteTag
import org.tasks.api.deleteTask
import org.tasks.api.findAccounts
import org.tasks.api.findLists
import org.tasks.api.findPlaces
import org.tasks.api.findReminders
import org.tasks.api.findTags
import org.tasks.api.findTasks
import org.tasks.api.setTaskReminders
import org.tasks.api.setTaskTags
import org.tasks.api.taskRow
import org.tasks.api.updateTasks

class DatabaseTasksApi(
    private val engine: ApiQueryEngine,
    private val writer: ApiWriter,
) {

    suspend fun queryTasks(query: TaskQuery = TaskQuery()): ApiPage<TaskRow> = io { engine.findTasks(query) }

    suspend fun countTasks(query: TaskQuery = TaskQuery()): Int = io { engine.countTasks(query) }

    suspend fun getTask(id: Long): TaskRow? = io { engine.taskRow(id) }

    suspend fun queryReminders(query: ReminderQuery = ReminderQuery()): ApiPage<ReminderRow> = io {
        engine.findReminders(query)
    }

    suspend fun queryLists(query: ListQuery = ListQuery()): ApiPage<ListRow> = io { engine.findLists(query) }

    suspend fun queryTags(query: TagQuery = TagQuery()): ApiPage<TagRow> = io { engine.findTags(query) }

    suspend fun queryPlaces(query: PlaceQuery = PlaceQuery()): ApiPage<PlaceRow> = io { engine.findPlaces(query) }

    suspend fun queryAccounts(query: AccountQuery = AccountQuery()): ApiPage<AccountRow> = io {
        engine.findAccounts(query)
    }

    suspend fun lists(): List<ListRow> = queryLists(ListQuery(limit = TasksContract.MAX_LIMIT)).rows

    suspend fun accounts(): List<AccountRow> =
        queryAccounts(AccountQuery(limit = TasksContract.MAX_LIMIT)).rows

    suspend fun createTasks(tasks: List<TaskWrite>): Creation = io { engine.createTasks(writer, tasks) }

    suspend fun updateTasks(updates: List<TaskUpdate>): Revision = io { engine.updateTasks(writer, updates) }

    suspend fun completeTasks(
        ids: List<Long>,
        completed: Boolean,
        completedAt: Any? = null,
    ): Completion = io { engine.completeTasks(writer, ids, completed, completedAt) }

    suspend fun deleteTask(id: Long): Deletion = io { engine.deleteTask(writer, id) }

    suspend fun deleteList(id: Long): Deletion = io { engine.deleteList(writer, id) }

    suspend fun deleteTag(id: Long): Deletion = io { engine.deleteTag(writer, id) }

    suspend fun deletePlace(id: Long): Deletion = io { engine.deletePlace(writer, id) }

    suspend fun createList(write: ListWrite): ListRow = io { engine.createList(writer, write) }

    suspend fun updateList(id: Long, write: ListWrite): ListRow = io { engine.changeList(writer, id, write) }

    suspend fun createTag(write: TagWrite): Created<TagRow> = io { engine.createTag(writer, write) }

    suspend fun updateTag(id: Long, write: TagWrite): TagRow = io { engine.changeTag(writer, id, write) }

    suspend fun setTaskTags(
        taskIds: List<Long>,
        add: List<Long>,
        remove: List<Long>,
    ): TagChange = io { engine.setTaskTags(writer, taskIds, add, remove) }

    suspend fun createPlace(write: PlaceWrite): Created<PlaceRow> = io { engine.createPlace(writer, write) }

    suspend fun updatePlace(id: Long, write: PlaceWrite): PlaceRow = io { engine.changePlace(writer, id, write) }

    suspend fun setTaskReminders(
        taskId: Long,
        add: List<ReminderWrite>,
        removeReminderIds: List<Long>,
    ): ReminderEdit = io { engine.setTaskReminders(writer, taskId, add, removeReminderIds) }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
}
