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

@Suppress("LongParameterList", "TooManyFunctions")
class TodoistSynchronizer(
    private val caldavDao: CaldavDao,
    private val taskDao: TaskDao,
    private val dirtyDao: DirtyDao,
    private val taskSaver: TaskSaver,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val taskDeleter: TaskDeleter,
    private val reporting: Reporting,
    private val clientProvider: TodoistClientProvider,
    private val tagDao: TagDao,
    private val tagDataDao: TagDataDao,
    private val vtodoCache: VtodoCache,
    private val createTask: suspend () -> Task,
    private val setDefaultList: suspend (CaldavFilter) -> Unit,
) {
    suspend fun sync(account: CaldavAccount) {
        Logger.d(TAG) { "Synchronizing $account" }
        if (!clientProvider.hasCredentials(account)) {
            setError(account, getString(Res.string.cannot_access_account))
            return
        }
        try {
            val initial = account.lastSync == 0L
            val pushError = synchronize(account)
            if (initial) {
                reporting.logEvent(
                    INITIAL_SYNC_COMPLETE,
                    PARAM_TYPE to Constants.SYNC_TYPE_TODOIST,
                    PARAM_TASK_COUNT to caldavDao.getTaskCountForAccount(account.uuid!!),
                )
            }
            account.lastSync = currentTimeMillis()
            caldavDao.setLastSync(account.id, account.lastSync)
            setError(account, pushError ?: "")
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            setError(account, e.message)
        } catch (e: SSLException) {
            setError(account, e.message)
        } catch (e: ConnectException) {
            setError(account, e.message)
        } catch (e: UnknownHostException) {
            setError(account, e.message)
        } catch (e: UnauthorizedException) {
            setError(account, e.message)
        } catch (e: ServiceUnavailableException) {
            setError(account, e.message)
        } catch (e: KeyManagementException) {
            setError(account, e.message)
        } catch (e: NoSuchAlgorithmException) {
            setError(account, e.message)
        } catch (e: IOException) {
            setError(account, e.message)
        } catch (e: HttpException) {
            if (e.code != 401 && e.code != 403) {
                reporting.reportException(e)
            }
            setError(account, e.message)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            setError(account, e.message)
            reporting.reportException(e)
        }
    }

    private suspend fun synchronize(account: CaldavAccount): String? {
        val service = clientProvider.getService(account)

        val token = account.syncToken?.takeIf { it.isNotBlank() } ?: TodoistService.FULL_SYNC
        var response = service.sync(syncToken = token)
        applyRemote(account, response)
        persistToken(account, response.syncToken)

        var pushError: String? = null
        var iteration = 0
        while (iteration++ < MAX_SYNC_ITERATIONS) {
            val pending = buildPush(account)
            if (pending.isEmpty()) {
                break
            }
            Logger.d(TAG) { "pushing ${pending.size} command(s), iteration $iteration" }
            var pushed = false
            pending.chunked(MAX_COMMANDS_PER_REQUEST).forEach { chunk ->
                response = service.sync(
                    syncToken = account.syncToken ?: TodoistService.FULL_SYNC,
                    commands = chunk.map { it.command },
                )
                pushError = pushError ?: handlePushResult(chunk, response)
                applyRemote(account, response)
                persistToken(account, response.syncToken)
                pushed = true
            }
            if (!pushed || pushError != null) {
                break
            }
        }
        return pushError
    }

    private suspend fun persistToken(account: CaldavAccount, syncToken: String) {
        if (account.syncToken == syncToken) {
            return
        }
        account.syncToken = syncToken
        caldavDao.setSyncToken(account.id, syncToken)
    }

    // region pull

    private suspend fun applyRemote(account: CaldavAccount, response: TodoistSyncResponse) {
        val projects = applyProjects(account, response)

        val lists = caldavDao
            .getCalendarsByAccount(account.uuid!!)
            .associateBy { it.uuid!! }

        if (response.items.isNotEmpty()) {
            val (deleted, live) = response.items.partition { it.isDeleted }
            deleteItems(lists, deleted)

            val touchedLists = mutableSetOf<String>()
            live.forEach { item ->
                val listUuid = item.projectId?.let { lists[it]?.uuid }
                if (listUuid == null) {
                    Logger.w(TAG) { "No local list for project ${item.projectId}, skipping ${item.id}" }
                    return@forEach
                }
                if (updateTask(lists.getValue(listUuid), item)) {
                    touchedLists.add(listUuid)
                }
            }
            touchedLists.forEach { caldavDao.updateParents(it) }
        }

        if (response.fullSync) {
            reapMissingTasks(account, lists.values, response.items)
        }
        if (projects.isNotEmpty() || response.items.isNotEmpty()) {
            refreshBroadcaster.broadcastRefresh()
        }
    }

    private suspend fun applyProjects(
        account: CaldavAccount,
        response: TodoistSyncResponse,
    ): List<TodoistProject> {
        val projects = response.projects
        if (projects.isEmpty() && !response.fullSync) {
            return emptyList()
        }
        val (gone, live) = projects.partition { it.isDeleted || it.isArchived }

        for (project in gone) {
            caldavDao.getCalendarByUrl(account.uuid!!, project.id)?.let {
                Logger.d(TAG) { "Deleting list for removed project ${project.id}" }
                taskDeleter.delete(it)
            }
        }

        if (response.fullSync) {
            val remoteIds = live.map { it.id }
            for (calendar in caldavDao.findDeletedCalendars(account.uuid!!, remoteIds)) {
                Logger.d(TAG) { "Deleting list absent from full sync: ${calendar.uuid}" }
                taskDeleter.delete(calendar)
            }
        }

        for (project in live) {
            val existing = caldavDao.getCalendarByUrl(account.uuid!!, project.id)
            if (existing == null) {
                val calendar = CaldavCalendar(account = account.uuid)
                    .apply { project.applyTo(this) }
                caldavDao.insert(calendar)
                if (project.isInboxProject) {
                    setDefaultList(CaldavFilter(calendar, account))
                }
            } else {
                val updated = existing.copy().apply { project.applyTo(this) }
                if (updated.name != existing.name ||
                    updated.access != existing.access ||
                    updated.color != existing.color
                ) {
                    project.applyTo(existing)
                    caldavDao.update(existing)
                }
            }
        }
        return live
    }

    private suspend fun deleteItems(
        lists: Map<String, CaldavCalendar>,
        deleted: List<TodoistItem>,
    ) {
        if (deleted.isEmpty()) {
            return
        }
        val taskIds = deleted
            .flatMap { item ->
                val listUuid = item.projectId?.let { lists[it]?.uuid }
                val caldavTasks = if (listUuid != null) {
                    caldavDao.getTaskByRemoteId(listUuid, item.id)?.let { listOf(it) }.orEmpty()
                } else {
                    lists.values.mapNotNull { caldavDao.getTaskByRemoteId(it.uuid!!, item.id) }
                }
                caldavTasks.map { it.task }
            }
            .flatMap { taskDao.getChildren(it) + it }
            .distinct()
        if (taskIds.isNotEmpty()) {
            Logger.d(TAG) { "Deleting ${taskIds.size} task(s) removed remotely" }
            taskDeleter.delete(taskIds)
        }
    }

    private suspend fun updateTask(list: CaldavCalendar, remote: TodoistItem): Boolean {
        val existing = caldavDao.getTaskByRemoteId(list.uuid!!, remote.id)
        val task = existing?.task?.let { taskDao.fetch(it) }
            ?: createTask().apply { taskDao.createNew(this) }
        val caldavTask = existing
            ?.copy(task = task.id)
            ?: CaldavTask(
                task = task.id,
                calendar = list.uuid,
                remoteId = remote.id,
                obj = "${remote.id}.json",
            )
        val isNew = existing == null
        if (!isNew && dirtyDao.isDirty(caldavTask.id) == true) {
            Logger.d(TAG) { "Ignoring remote update for dirty taskId=${task.id}" }
            return false
        }
        val original = task.copy()
        task.applyRemote(remote)
        task.suppressSync()
        task.suppressRefresh()
        taskSaver.save(task, original, dirty = false)
        vtodoCache.putVtodo(list, caldavTask, json.encodeToString(remote))
        tagDao.applyTags(task, remote.labels)
        caldavTask.remoteParent = remote.parentId ?: ""
        caldavTask.calendar = list.uuid
        caldavDao.insertOrUpdateAndMarkSynced(caldavTask)
        return true
    }

    private suspend fun reapMissingTasks(
        account: CaldavAccount,
        lists: Collection<CaldavCalendar>,
        items: List<TodoistItem>,
    ) {
        val present = items.map { it.id }.toSet()
        val reap = mutableListOf<Long>()
        for (list in lists) {
            val candidates = caldavDao.getTasksByRemoteId(
                list.uuid!!,
                caldavDao.getTopLevelRemoteIds(list.uuid!!).filterNotNull(),
            )
            for (caldavTask in candidates) {
                val remoteId = caldavTask.remoteId ?: continue
                if (remoteId in present) {
                    continue
                }
                if ((dirtyDao.getSyncedVersion(caldavTask.id) ?: 0L) == 0L) {
                    continue
                }
                if (dirtyDao.isDirty(caldavTask.id) == true) {
                    continue
                }
                val task = taskDao.fetch(caldavTask.task) ?: continue
                if (task.isCompleted) {
                    continue
                }
                reap.add(caldavTask.task)
            }
        }
        if (reap.isNotEmpty()) {
            Logger.d(TAG) { "Reaping ${reap.size} task(s) absent from full sync of ${account.uuid}" }
            taskDeleter.delete(reap.flatMap { taskDao.getChildren(it) + it }.distinct())
        }
    }

    // endregion

    // region push

    private data class Pending(
        val command: TodoistCommand,
        val caldavTaskId: Long,
        val dirtyVersion: Long,
        val taskId: Long,
        val calendar: String,
        val tempId: String? = null,
        val deletesLocally: Boolean = false,
        val dropsJoinRow: Boolean = false,
    )

    private suspend fun buildPush(account: CaldavAccount): List<Pending> {
        val lists = caldavDao.getCalendarsByAccount(account.uuid!!)
        val pending = mutableListOf<Pending>()

        for (list in lists) {
            for (moved in caldavDao.getMoved(list.uuid!!)) {
                val remoteId = moved.remoteId
                if (remoteId.isNullOrBlank()) {
                    caldavDao.delete(moved)
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

            val toPush = dirtyDao.getTasksToPush(list.uuid!!).sortedByDepth()
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
        val caldavTask = caldavDao.getCaldavTaskById(taskToPush.caldavTaskId) ?: return emptyList()
        if (task.isDeleted) {
            return deleteCommands(list, taskToPush, caldavTask)
        }
        val context = PushContext(
            list = list,
            taskToPush = taskToPush,
            caldavTask = caldavTask,
            tags = tagDataDao.getTagDataForTask(task.id),
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
            taskDeleter.delete(taskDao.getChildren(task.id) + task.id)
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

    private fun PushContext.createCommands(): List<Pending> {
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

    private fun PushContext.updateCommands(): List<Pending> {
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

    private fun PushContext.moveCommand(remoteId: String): Pending? {
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

    private fun PushContext.completionCommand(remoteId: String): Pending? {
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

    private class PushContext(
        val list: CaldavCalendar,
        val taskToPush: TaskToPush,
        val caldavTask: CaldavTask,
        val tags: List<TagData>,
        val cached: TodoistItem?,
        val parentRemoteId: String?,
    ) {
        val task: Task get() = taskToPush.task
    }

    private fun pending(
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

    private suspend fun parentReference(task: Task, earlier: List<Pending>): String? {
        if (task.parent <= 0L) {
            return null
        }
        earlier.firstOrNull { it.taskId == task.parent && it.tempId != null }?.let {
            return it.tempId
        }
        return caldavDao.getTask(task.parent)?.remoteId?.takeIf { it.isNotBlank() }
    }

    private suspend fun cachedItem(list: CaldavCalendar, caldavTask: CaldavTask): TodoistItem? =
        vtodoCache
            .getVtodo(list, caldavTask)
            ?.let { runCatching { json.decodeFromString<TodoistItem>(it) }.getOrNull() }

    private suspend fun handlePushResult(
        chunk: List<Pending>,
        response: TodoistSyncResponse,
    ): String? {
        var firstError: String? = null
        for (item in chunk) {
            when (val result = response.statusOf(item.command.uuid)) {
                TodoistSyncResponse.CommandResult.Ok -> onPushSucceeded(item, response)
                TodoistSyncResponse.CommandResult.Missing -> {
                    Logger.w(TAG) { "No sync_status for ${item.command.type} ${item.command.uuid}" }
                }
                is TodoistSyncResponse.CommandResult.Error ->
                    firstError = onPushFailed(item, result) ?: firstError
            }
        }
        return firstError
    }

    private suspend fun onPushFailed(
        item: Pending,
        result: TodoistSyncResponse.CommandResult.Error,
    ): String? {
        if (result.isNotFound) {
            Logger.w(TAG) {
                "${item.command.type} for taskId=${item.taskId} gone remotely: ${result.message}"
            }
            taskDeleter.delete(taskDao.getChildren(item.taskId) + item.taskId)
            return null
        }
        Logger.e(TAG) {
            "${item.command.type} failed for taskId=${item.taskId}: ${result.message}"
        }
        reporting.reportException(
            IllegalStateException("Todoist ${item.command.type}: ${result.message}")
        )
        return "${item.command.type}: ${result.message}"
    }

    private suspend fun onPushSucceeded(item: Pending, response: TodoistSyncResponse) {
        if (item.deletesLocally) {
            taskDeleter.delete(taskDao.getChildren(item.taskId) + item.taskId)
            return
        }
        val caldavTask = caldavDao.getCaldavTaskById(item.caldavTaskId)
        if (caldavTask == null) {
            return
        }
        item.tempId?.let { tempId ->
            val assigned = response.tempIdMapping[tempId]
            if (assigned == null) {
                Logger.e(TAG) { "No id returned for temp_id $tempId on taskId=${item.taskId}" }
                reporting.reportException(
                    IllegalStateException("Todoist item_add returned no temp_id mapping")
                )
                return
            }
            caldavTask.remoteId = assigned
            caldavTask.obj = "$assigned.json"
        }
        val command = item.command
        if (command.type == TodoistCommand.ITEM_MOVE) {
            caldavTask.remoteParent =
                command.args["parent_id"]?.let { it.toString().trim('"') } ?: ""
        }
        if (item.dropsJoinRow) {
            caldavDao.delete(caldavTask)
            return
        }
        caldavDao.update(caldavTask)
        dirtyDao.markPushed(item.caldavTaskId, item.dirtyVersion)
    }

    // endregion

    private suspend fun setError(account: CaldavAccount, message: String?) {
        account.error = message
        caldavDao.setError(account.id, message)
        refreshBroadcaster.broadcastRefresh()
        if (!message.isNullOrEmpty()) {
            Logger.e(TAG) { message }
        }
    }

    companion object {
        private const val TAG = "TodoistSynchronizer"
        private const val MAX_SYNC_ITERATIONS = 3

        private const val MAX_COMMANDS_PER_REQUEST = 100

        private val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
    }
}
