package org.tasks.sync.todoist
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.tasks.analytics.AnalyticsEvents.INITIAL_SYNC_COMPLETE
import org.tasks.analytics.AnalyticsEvents.PARAM_TASK_COUNT
import org.tasks.analytics.AnalyticsEvents.PARAM_TYPE
import org.tasks.analytics.Constants
import org.tasks.analytics.Reporting
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.VtodoCache
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.DirtyDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.TaskToPush
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.http.HttpException
import org.tasks.http.NotFoundException
import org.tasks.http.ServiceUnavailableException
import org.tasks.http.UnauthorizedException
import org.tasks.resources.getString
import org.tasks.service.TaskDeleter
import org.tasks.sync.todoist.TodoistConverter.applyRemote
import org.tasks.sync.todoist.TodoistConverter.toArgs
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cannot_access_account
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLException

data class Pending(
    val command: TodoistCommand,
    val caldavTaskId: Long,
    val dirtyVersion: Long,
    val taskId: Long,
    val calendar: String,
    val tempId: String? = null,
    val deletesLocally: Boolean = false,
    val dropsJoinRow: Boolean = false,
)


class PushContext(
    val list: CaldavCalendar,
    val taskToPush: TaskToPush,
    val caldavTask: CaldavTask,
    val tags: List<TagData>,
    val cached: TodoistItem?,
    val parentRemoteId: String?,
) {
    val task: Task get() = taskToPush.task

    fun createCommands(): List<Pending> {
        val commands = mutableListOf<Pending>()
        val tempId = todoistCommandId()
        commands.add(
            pending(
                list = list,
                taskToPush = taskToPush,
                command = TodoistCommand(
                    type = TodoistCommand.ITEM_ADD,
                    uuid = todoistCommandId(),
                    tempId = tempId,
                    args = task.toArgs(
                        tags = tags,
                        target = TodoistConverter.ArgTarget(
                            projectId = list.uuid,
                            sectionId = cached?.sectionId,
                            parentId = parentRemoteId,
                            previousDueString = cached?.due?.string,
                        ),
                    ),
                ),
                tempId = tempId,
            )
        )
        if (task.isCompleted) {
            commands.add(
                pending(
                    list = list,
                    taskToPush = taskToPush,
                    command = TodoistCommand(
                        type = TodoistCommand.ITEM_COMPLETE,
                        uuid = todoistCommandId(),
                        args = buildJsonObject { put("id", tempId) },
                    ),
                )
            )
        }
        return commands
    }

    fun updateCommands(): List<Pending> {
        val commands = mutableListOf<Pending>()
        val remoteId = caldavTask.remoteId!!
        commands.add(
            pending(
                list = list,
                taskToPush = taskToPush,
                command = TodoistCommand(
                    type = TodoistCommand.ITEM_UPDATE,
                    uuid = todoistCommandId(),
                    args = task.toArgs(
                        tags = tags,
                        target = TodoistConverter.ArgTarget(
                            remoteId = remoteId,
                            previousDueString = cached?.due?.string,
                        ),
                    ),
                ),
            )
        )

        moveCommand(remoteId)?.let { commands.add(it) }
        completionCommand(remoteId)?.let { commands.add(it) }
        return commands
    }

    fun moveCommand(remoteId: String): Pending? {
        val lastSyncedParent = caldavTask.remoteParent?.takeIf { it.isNotBlank() }
        if (parentRemoteId == lastSyncedParent) {
            return null
        }
        return pending(
            list = list,
            taskToPush = taskToPush,
            command = TodoistCommand(
                type = TodoistCommand.ITEM_MOVE,
                uuid = todoistCommandId(),
                args = buildJsonObject {
                    put("id", remoteId)
                    if (parentRemoteId != null) {
                        put("parent_id", parentRemoteId)
                    } else {
                        put("project_id", list.uuid!!)
                    }
                },
            ),
        )
    }

    fun completionCommand(remoteId: String): Pending? {
        val wasCompleted = cached?.checked ?: false
        if (task.isCompleted == wasCompleted) {
            return null
        }
        return pending(
            list = list,
            taskToPush = taskToPush,
            command = TodoistCommand(
                type = if (task.isCompleted) {
                    TodoistCommand.ITEM_COMPLETE
                } else {
                    TodoistCommand.ITEM_UNCOMPLETE
                },
                uuid = todoistCommandId(),
                args = buildJsonObject { put("id", remoteId) },
            ),
        )
    }
}

internal fun pending(
    list: CaldavCalendar,
    taskToPush: TaskToPush,
    command: TodoistCommand,
    tempId: String? = null,
    deletesLocally: Boolean = false,
) = Pending(
    command = command,
    caldavTaskId = taskToPush.caldavTaskId,
    dirtyVersion = taskToPush.dirtyVersion,
    taskId = taskToPush.task.id,
    calendar = list.uuid!!,
    tempId = tempId,
    deletesLocally = deletesLocally,
)


class TodoistPushCommands(
    private val stores: TodoistSyncStores,
    private val services: TodoistSyncServices,
    private val hooks: TodoistSyncHooks,
) {
    suspend fun buildPush(account: CaldavAccount): List<Pending> {
        val lists = stores.caldavDao.getCalendarsByAccount(account.uuid!!)
        val pending = mutableListOf<Pending>()

        for (list in lists) {
            for (moved in stores.caldavDao.getMoved(list.uuid!!)) {
                val remoteId = moved.remoteId
                if (remoteId.isNullOrBlank()) {
                    stores.caldavDao.delete(moved)
                    continue
                }
                pending.add(
                    Pending(
                        command = TodoistCommand(
                            type = TodoistCommand.ITEM_DELETE,
                            uuid = todoistCommandId(),
                            args = buildJsonObject { put("id", remoteId) },
                        ),
                        caldavTaskId = moved.id,
                        dirtyVersion = 0L,
                        taskId = moved.task,
                        calendar = list.uuid!!,
                        dropsJoinRow = true,
                    )
                )
            }

            val toPush = stores.dirtyDao.getTasksToPush(list.uuid!!).sortedByDepth()
            for (taskToPush in toPush) {
                pending.addAll(commandsFor(list, taskToPush, pending))
            }
        }
        return pending
    }

    private fun List<TaskToPush>.sortedByDepth(): List<TaskToPush> {
        val byId = associateBy { it.task.id }
        val depths = mutableMapOf<Long, Int>()
        fun depth(id: Long, seen: Set<Long> = emptySet()): Int {
            depths[id]?.let { return it }
            if (id in seen) {
                return 0
            }
            val parent = byId[id]?.task?.parent ?: 0L
            val result = if (parent == 0L) 0 else depth(parent, seen + id) + 1
            depths[id] = result
            return result
        }
        return sortedBy { depth(it.task.id) }
    }

    private suspend fun commandsFor(
        list: CaldavCalendar,
        taskToPush: TaskToPush,
        earlier: List<Pending>,
    ): List<Pending> {
        val task = taskToPush.task
        val caldavTask = stores.caldavDao.getCaldavTaskById(taskToPush.caldavTaskId) ?: return emptyList()
        if (task.isDeleted) {
            return deleteCommands(list, taskToPush, caldavTask)
        }
        val context = PushContext(
            list = list,
            taskToPush = taskToPush,
            caldavTask = caldavTask,
            tags = stores.tagDataDao.getTagDataForTask(task.id),
            cached = cachedItem(list, caldavTask),
            parentRemoteId = parentReference(task, earlier),
        )
        val isNew = taskToPush.syncedVersion == 0L || caldavTask.remoteId.isNullOrBlank()
        return if (isNew) context.createCommands() else context.updateCommands()
    }

    private suspend fun deleteCommands(
        list: CaldavCalendar,
        taskToPush: TaskToPush,
        caldavTask: CaldavTask,
    ): List<Pending> {
        val task = taskToPush.task
        val remoteId = caldavTask.remoteId
        if (remoteId.isNullOrBlank() || taskToPush.syncedVersion == 0L) {
            services.taskDeleter.delete(stores.taskDao.getChildren(task.id) + task.id)
            return emptyList()
        }
        return listOf(
            pending(
                list = list,
                taskToPush = taskToPush,
                command = TodoistCommand(
                    type = TodoistCommand.ITEM_DELETE,
                    uuid = todoistCommandId(),
                    args = buildJsonObject { put("id", remoteId) },
                ),
                deletesLocally = true,
            )
        )
    }

    private suspend fun parentReference(task: Task, earlier: List<Pending>): String? {
        if (task.parent <= 0L) {
            return null
        }
        earlier.firstOrNull { it.taskId == task.parent && it.tempId != null }?.let {
            return it.tempId
        }
        return stores.caldavDao.getTask(task.parent)?.remoteId?.takeIf { it.isNotBlank() }
    }

    private suspend fun cachedItem(list: CaldavCalendar, caldavTask: CaldavTask): TodoistItem? =
        hooks.vtodoCache
            .getVtodo(list, caldavTask)
            ?.let { runCatching { json.decodeFromString<TodoistItem>(it) }.getOrNull() }
}

private const val TAG = "TodoistSynchronizer"

private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}
