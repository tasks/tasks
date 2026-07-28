package org.tasks.sync.microsoft

import co.touchlab.kermit.Logger
import io.ktor.client.call.body
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
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
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_WRITE
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_UNKNOWN
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.http.HttpException
import org.tasks.http.NetworkException
import org.tasks.http.NotFoundException
import org.tasks.http.ServiceUnavailableException
import org.tasks.http.UnauthorizedException
import org.jetbrains.compose.resources.getString
import org.tasks.preferences.AppPreferences
import org.tasks.service.TaskDeleter
import org.tasks.sync.microsoft.MicrosoftConverter.applyRemote
import org.tasks.sync.microsoft.MicrosoftConverter.applySubtask
import org.tasks.sync.microsoft.MicrosoftConverter.toChecklistItem
import org.tasks.sync.microsoft.MicrosoftConverter.toRemote
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLException
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cannot_access_account


class MicrosoftSynchronizer(
    private val caldavDao: CaldavDao,
    private val taskDao: TaskDao,
    private val dirtyDao: DirtyDao,
    private val taskSaver: TaskSaver,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val taskDeleter: TaskDeleter,
    private val reporting: Reporting,
    private val clientProvider: MicrosoftClientProvider,
    private val tagDao: TagDao,
    private val tagDataDao: TagDataDao,
    private val appPreferences: AppPreferences,
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
            synchronize(account)
            if (account.lastSync == 0L) {
                val taskCount = caldavDao.getTaskCountForAccount(account.uuid!!)
                reporting.logEvent(
                    INITIAL_SYNC_COMPLETE,
                    PARAM_TYPE to Constants.SYNC_TYPE_MICROSOFT,
                    PARAM_TASK_COUNT to taskCount
                )
            }
            account.lastSync = currentTimeMillis()
            caldavDao.setLastSync(account.id, account.lastSync)
            setError(account, "")
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
            val message = when (e.code) {
                402 -> e.message
                else -> {
                    reporting.reportException(e)
                    e.message
                }
            }
            setError(account, message)
        } catch (e: Exception) {
            setError(account, e.message)
            reporting.reportException(e)
        }
    }

    private suspend fun synchronize(account: CaldavAccount) {
        Logger.d(TAG) { "Synchronize $account" }
        val microsoft = clientProvider.getService(account)
        val taskLists = getTaskLists(microsoft)
        for (calendar in caldavDao.findDeletedCalendars(account.uuid!!, taskLists.map { it.id!! })) {
            taskDeleter.delete(calendar)
        }
        for (remote in taskLists) {
            var local = caldavDao.getCalendarByUrl(account.uuid!!, remote.id!!)
            val remoteName = remote.displayName
            val access = when {
                remote.isOwner == true -> ACCESS_OWNER
                remote.isShared == true -> ACCESS_READ_WRITE
                else -> ACCESS_UNKNOWN
            }
            if (local == null) {
                local = CaldavCalendar(
                    account = account.uuid,
                ).apply {
                    remote.applyTo(this)
                }
                caldavDao.insert(local)
                if (remote.isDefaultList) {
                    setDefaultList(CaldavFilter(local, account))
                }
            } else if (local.name != remoteName || local.access != access) {
                remote.applyTo(local)
                caldavDao.update(local)
                refreshBroadcaster.broadcastRefresh()
            }
            syncList(local, remote, microsoft)
            caldavDao.updateParents(local.uuid!!)
        }
    }

    private suspend fun syncList(
        local: CaldavCalendar,
        remote: TaskLists.TaskList,
        microsoft: MicrosoftService,
        iteration: Int = 0,
    ) {
        if (iteration > MAX_SYNC_ITERATIONS) {
            Logger.e(TAG) { "Reached max sync iterations for $local" }
            return
        }
        if (iteration > 0) {
            Logger.d(TAG) { "syncList iteration ${iteration + 1} for $local" }
        }
        if (local.ctag?.isNotBlank() == true) {
            deltaSync(local, remote, microsoft)
        } else {
            fullSync(local, remote, microsoft)
        }
        if (pushLocalChanges(local, microsoft)) {
            syncList(local, remote, microsoft, iteration + 1)
        }
    }

    private suspend fun pushLocalChanges(
        local: CaldavCalendar,
        microsoft: MicrosoftService,
    ): Boolean {
        val moved = caldavDao.getMoved(local.uuid!!)
        for (task in moved) {
            deleteRemoteResource(microsoft, local, task)
        }
        val toPush = dirtyDao.getTasksToPush(local.uuid!!)
        for (taskToPush in toPush) {
            val task = taskToPush.task
            try {
                pushTask(local, taskToPush, microsoft)
            } catch (e: NotFoundException) {
                Logger.w(TAG, e) { "Task ${task.id} gone remotely, deleting locally" }
                taskDeleter.delete(taskDao.getChildren(task.id) + task.id)
            }
        }
        return moved.isNotEmpty() || toPush.isNotEmpty()
    }

    private suspend fun deleteRemoteResource(
        microsoft: MicrosoftService,
        list: CaldavCalendar,
        task: CaldavTask,
        syncedVersion: Long? = null,
    ): Boolean {
        val listId = list.uuid
        val parentId = task.remoteParent
        val taskId = task.remoteId
        val success = when {
            syncedVersion == 0L -> true
            listId.isNullOrBlank() -> false
            taskId.isNullOrBlank() -> false
            parentId.isNullOrBlank() -> {
                try {
                    microsoft.deleteTask(listId, taskId)
                } catch (e: NotFoundException) {
                    Logger.w(TAG, e) { "task=$task" }
                } catch (e: org.tasks.http.HttpException) {
                    when (e.code) {
                        400 -> Logger.w(TAG, e) { "task=$task" }
                        else -> {
                            throw e
                        }
                    }
                }
                true
            }
            else -> {
                try {
                    microsoft.deleteChecklistItem(listId, parentId, taskId)
                } catch (e: NotFoundException) {
                    Logger.w(TAG, e) { "task=$task" }
                } catch (e: org.tasks.http.HttpException) {
                    when (e.code) {
                        400 -> Logger.w(TAG, e) { "task=$task" }
                        else -> {
                            throw e
                        }
                    }
                }
                true
            }
        }
        if (success) {
            caldavDao.delete(task)
        }
        return success
    }

    private suspend fun pushTask(
        list: CaldavCalendar,
        taskToPush: TaskToPush,
        microsoft: MicrosoftService,
    ) {
        val task = taskToPush.task
        val caldavTask = caldavDao.getCaldavTaskById(taskToPush.caldavTaskId) ?: return
        if (task.isDeleted) {
            Logger.d(TAG) { "Deleting $task" }
            if (deleteRemoteResource(microsoft, list, caldavTask, taskToPush.syncedVersion)) {
                taskDeleter.delete(taskDao.getChildren(task.id) + task.id)
            }
            return
        }
        dirtyDao.withDirtyVersion(taskToPush.caldavTaskId, taskToPush.dirtyVersion) {
            var isNew = taskToPush.syncedVersion == 0L
            val isSubtask = task.parent > 0L
            val newParentRemoteId = if (isSubtask) {
                caldavDao.getTask(task.parent)?.remoteId
                    ?: error("Missing parent remote ID for task ${task.id}")
            } else null
            // Detect hierarchy changes by comparing the current local parent with
            // the last-synced remoteParent. The adapter preserves remoteParent for
            // Microsoft tasks so it reflects the last-synced state, not the pending state.
            if (!isNew) {
                val wasSubtask = !caldavTask.remoteParent.isNullOrBlank()
                val hierarchyChanged = isSubtask != wasSubtask ||
                        (isSubtask && newParentRemoteId != caldavTask.remoteParent)
                if (hierarchyChanged) {
                    Logger.d(TAG) { "Hierarchy changed for $task (wasSubtask=$wasSubtask, isSubtask=$isSubtask)" }
                    try {
                        if (wasSubtask) {
                            microsoft.deleteChecklistItem(
                                list.uuid!!, caldavTask.remoteParent!!, caldavTask.remoteId!!
                            )
                        } else {
                            microsoft.deleteTask(list.uuid!!, caldavTask.remoteId!!)
                        }
                    } catch (e: NotFoundException) {
                        Logger.w(TAG, e) { "Old remote object already deleted" }
                    } catch (e: org.tasks.http.HttpException) {
                        when (e.code) {
                            400 -> Logger.w(TAG, e) { "Failed to delete old remote object" }
                            else -> throw e
                        }
                    }
                    vtodoCache.delete(list, caldavTask)
                    caldavTask.remoteId = null
                    isNew = true
                }
            }
            if (!isSubtask) {
                val remoteTask = task.toRemote(
                    caldavTask = caldavTask,
                    tags = tagDataDao.getTagDataForTask(task.id),
                )
                val result: Tasks.Task = if (isNew) {
                    Logger.d(TAG) { "Uploading new task: $task" }
                    microsoft.createTask(list.uuid!!, remoteTask)
                } else {
                    Logger.d(TAG) { "Updating existing task: $task" }
                    microsoft.updateTask(list.uuid!!, caldavTask.remoteId!!, remoteTask)
                }
                caldavTask.remoteId = result.id
                caldavTask.remoteParent = ""
                caldavTask.obj = "${result.id}.json"
                caldavTask.etag = result.etag
                vtodoCache.putVtodo(list, caldavTask, json.encodeToString(result))
            } else {
                val caldavParent = newParentRemoteId!!
                val remoteTask = task.toChecklistItem(caldavTask.remoteId)
                val result: Tasks.Task.ChecklistItem = if (isNew) {
                    Logger.d(TAG) { "Uploading new checklist item: $task" }
                    microsoft.createChecklistItem(list.uuid!!, caldavParent, remoteTask)
                } else {
                    Logger.d(TAG) { "Updating existing checklist item: $task" }
                    microsoft.updateChecklistItem(list.uuid!!, caldavParent, remoteTask)
                }
                caldavTask.remoteId = result.id
                caldavTask.remoteParent = caldavParent
                caldavTask.obj = "${result.id}.json"
                vtodoCache.putVtodo(list, caldavTask, json.encodeToString(result))
            }

            caldavDao.update(caldavTask)
        }
    }

    private suspend fun deltaSync(
        list: CaldavCalendar,
        remoteList: TaskLists.TaskList,
        microsoft: MicrosoftService
    ) {
        Logger.d(TAG) { "delta update: $list" }
        val tasks = getTasks(list, remoteList, microsoft) ?: return
        for (remote in tasks) {
            if (remote.removed == null) {
                updateTask(list, remote)
            } else {
                val caldavTasks = caldavDao.getTasksByRemoteId(list.uuid!!, listOf(remote.id!!))
                val taskIds = caldavTasks.map { it.task }.flatMap { taskDao.getChildren(it) + it }
                Logger.d(TAG) { "Deleting $remote, taskIds=$taskIds" }
                taskDeleter.delete(taskIds)
            }
        }
        Logger.d(TAG) { "UPDATE $list" }
        caldavDao.update(list)
        refreshBroadcaster.broadcastRefresh()
    }

    private suspend fun getTaskLists(
        microsoft: MicrosoftService,
    ): List<TaskLists.TaskList> {
        val taskLists = ArrayList<TaskLists.TaskList>()
        var nextPageToken: String? = null
        do {
            val response = if (nextPageToken == null) {
                microsoft.getLists()
            } else {
                microsoft.paginateLists(nextPageToken)
            }
            taskLists.addAll(response.value)
            nextPageToken = response.nextPage
            Logger.d(TAG) { "nextPageToken: $nextPageToken" }
        } while (nextPageToken?.isNotBlank() == true)
        Logger.d(TAG) { "response: $taskLists" }
        return taskLists
    }

    private suspend fun getTasks(
        local: CaldavCalendar,
        remoteList: TaskLists.TaskList,
        microsoft: MicrosoftService,
    ): List<Tasks.Task>? {
        val tasks = ArrayList<Tasks.Task>()
        var nextPageToken: String? = null
        do {
            val response = try {
                if (nextPageToken == null) {
                    local.ctag
                        ?.let { microsoft.paginateTasks(it) }
                        ?: microsoft.getTasks(remoteList.id!!)
                } else {
                    microsoft.paginateTasks(nextPageToken)
                }
            } catch (e: NetworkException) {
                val invalidDeltaToken = local.ctag != null &&
                    (e.graphCode == "syncStateNotFound" || e.graphCode == "ResourceNotFound")
                if (invalidDeltaToken) {
                    Logger.e(TAG) { "$local: delta token no longer valid, resetting ctag: ${e.message}" }
                    local.ctag = null
                    caldavDao.update(local)
                    return null
                }
                throw e
            }
            val body = response.body<Tasks>()
            tasks.addAll(body.value)
            nextPageToken = body.nextPage
            body.nextDelta?.let { local.ctag = it}
        } while (nextPageToken?.isNotBlank() == true)
        return tasks
    }

    private suspend fun fullSync(
        list: CaldavCalendar,
        remoteList: TaskLists.TaskList,
        microsoft: MicrosoftService,
    ) {
        Logger.d(TAG) { "full update: $list" }
        val tasks = getTasks(list, remoteList, microsoft) ?: return
        tasks.forEach { updateTask(list, it) }
        caldavDao
            .getTopLevelRemoteIds(list.uuid!!)
            .subtract(tasks.map { it.id }.toSet())
            .takeIf { it.isNotEmpty() }
            ?.let {
                Logger.d(TAG) { "DELETED $it" }
                val caldavTasks = caldavDao.getTasksByRemoteId(list.uuid!!, it.filterNotNull())
                val taskIds = caldavTasks.map { it.task }.flatMap { taskDao.getChildren(it) + it }
                taskDeleter.delete(taskIds)
            }
        Logger.d(TAG) { "UPDATE $list" }
        caldavDao.update(list)
        refreshBroadcaster.broadcastRefresh()
    }

    private suspend fun updateTask(list: CaldavCalendar, remote: Tasks.Task) {
        val existing = caldavDao.getTaskByRemoteId(list.uuid!!, remote.id!!)
        val task = existing?.task?.let { taskDao.fetch(it) }
            ?: createTask().apply {
                taskDao.createNew(this)
            }
        val caldavTask =
            existing
                ?.copy(task = task.id)
                ?: CaldavTask(
                    task = task.id,
                    calendar = list.uuid,
                    remoteId = remote.id,
                    obj = "${remote.id}.json",
                )
        val isNew = existing == null
        val dirty = !isNew && dirtyDao.isDirty(caldavTask.id) == true
        if (dirty) {
            // TODO: merge with vtodo cached value, similar to iCalendarMerge.kt
            Logger.w(TAG) { "Ignoring update for dirty taskId=${task.id} remote=$remote" }
            return
        }
        val original = task.copy()
        task.applyRemote(remote, appPreferences.defaultPriority())
        task.suppressSync()
        task.suppressRefresh()
        taskSaver.save(task, original, dirty = false)
        vtodoCache.putVtodo(list, caldavTask, json.encodeToString(remote))
        tagDao.applyTags(task, remote.categories ?: emptyList())
        caldavTask.remoteParent = ""
        syncChecklist(
            list = list,
            parentId = task.id,
            parentRemoteId = caldavTask.remoteId!!,
            parentCompletionDate = task.completionDate,
            checklistItems = remote.checklistItems ?: emptyList(),
        )
        caldavTask.etag = remote.etag
        caldavDao.insertOrUpdateAndMarkSynced(caldavTask)
    }

    private suspend fun syncChecklist(
        list: CaldavCalendar,
        parentId: Long,
        parentRemoteId: String,
        parentCompletionDate: Long,
        checklistItems: List<Tasks.Task.ChecklistItem>,
    ) {
        val localSubtasks: List<CaldavTask> = taskDao.getChildren(parentId).let { caldavDao.getTasks(it) }
        val remoteSubtaskIds = checklistItems.map { it.id }
        localSubtasks
            .filterNot {
                val isNew = (dirtyDao.getSyncedVersion(it.id) ?: 0L) == 0L
                isNew || it.remoteId.isNullOrBlank() || remoteSubtaskIds.contains(it.remoteId)
            }
            .takeIf { it.isNotEmpty() }
            ?.let { removedSubtasks ->
                taskDeleter.delete(removedSubtasks.map { it.task })
            }
        checklistItems.forEach { item ->
            val existing = caldavDao.getTaskByRemoteId(list.uuid!!, item.id!!)
            if (existing?.isDeleted() == true) {
                // Pending local delete — skip; pushLocalChanges will
                // remove this from the remote
                return@forEach
            }
            val task = existing?.task?.let { taskDao.fetch(it) }
                ?: createTask().apply {
                    taskDao.createNew(this)
                }
            val caldavTask =
                existing
                    ?.copy(task = task.id)
                    ?: CaldavTask(
                        task = task.id,
                        calendar = list.uuid,
                        remoteId = item.id,
                        remoteParent = parentRemoteId,
                        obj = "${item.id}.json",
                    )
            val isNew = existing == null
            val dirty = !isNew && dirtyDao.isDirty(caldavTask.id) == true
            val original = task.copy()
            if (dirty) {
                // Don't override task.parent for dirty tasks — the local
                // hierarchy change will be pushed in pushLocalChanges()
            } else {
                task.applySubtask(
                    parent = parentId,
                    parentCompletionDate = parentCompletionDate,
                    checklistItem = item,
                )
            }
            task.suppressSync()
            task.suppressRefresh()
            taskSaver.save(task, original, dirty = false)
            caldavDao.insertOrUpdateAndMarkSynced(caldavTask)
        }
    }

    private suspend fun setError(account: CaldavAccount, message: String?) {
        account.error = message
        caldavDao.setError(account.id, message)
        refreshBroadcaster.broadcastRefresh()
        if (!message.isNullOrEmpty()) {
            Logger.e(TAG) { message }
        }
    }

    companion object {
        private const val TAG = "MicrosoftSynchronizer"
        private const val MAX_SYNC_ITERATIONS = 3
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }
}
