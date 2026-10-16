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

class TodoistSyncStores(
    val caldavDao: CaldavDao,
    val taskDao: TaskDao,
    val dirtyDao: DirtyDao,
    val tagDao: TagDao,
    val tagDataDao: TagDataDao,
)

class TodoistSyncServices(
    val taskSaver: TaskSaver,
    val taskDeleter: TaskDeleter,
    val refreshBroadcaster: RefreshBroadcaster,
    val reporting: Reporting,
)

class TodoistSyncHooks(
    val vtodoCache: VtodoCache,
    val createTask: suspend () -> Task,
    val setDefaultList: suspend (CaldavFilter) -> Unit,
)

class TodoistSynchronizer(
    private val clientProvider: TodoistClientProvider,
    private val stores: TodoistSyncStores,
    private val services: TodoistSyncServices,
    private val hooks: TodoistSyncHooks,
    private val pushCommands: TodoistPushCommands,
    private val pushResults: TodoistPushResults,
) {
    suspend fun sync(account: CaldavAccount) {
        Logger.d(TAG) { "Synchronizing $account" }
        if (!clientProvider.hasCredentials(account)) {
            setError(account, getString(Res.string.cannot_access_account))
            return
        }
        val outcome = runCatching { syncOnce(account) }
        val failure = outcome.exceptionOrNull()
        if (failure is CancellationException) {
            throw failure
        }
        when (failure) {
            null -> setError(account, outcome.getOrNull() ?: "")
            else -> {
                if (shouldReport(failure)) {
                    services.reporting.reportException(failure)
                }
                setError(account, failure.message)
            }
        }
    }

    private suspend fun syncOnce(account: CaldavAccount): String? {
        val initial = account.lastSync == 0L
        val pushError = synchronize(account)
        if (initial) {
            services.reporting.logEvent(
                INITIAL_SYNC_COMPLETE,
                PARAM_TYPE to Constants.SYNC_TYPE_TODOIST,
                PARAM_TASK_COUNT to stores.caldavDao.getTaskCountForAccount(account.uuid!!),
            )
        }
        account.lastSync = currentTimeMillis()
        stores.caldavDao.setLastSync(account.id, account.lastSync)
        return pushError
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
            val pending = pushCommands.buildPush(account)
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
                pushError = pushError ?: pushResults.handlePushResult(chunk, response)
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
        stores.caldavDao.setSyncToken(account.id, syncToken)
    }

    // region pull

    private suspend fun applyRemote(account: CaldavAccount, response: TodoistSyncResponse) {
        val projects = applyProjects(account, response)

        val lists = stores.caldavDao
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
            touchedLists.forEach { stores.caldavDao.updateParents(it) }
        }

        if (response.fullSync) {
            reapMissingTasks(account, lists.values, response.items)
        }
        if (projects.isNotEmpty() || response.items.isNotEmpty()) {
            services.refreshBroadcaster.broadcastRefresh()
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
            stores.caldavDao.getCalendarByUrl(account.uuid!!, project.id)?.let {
                Logger.d(TAG) { "Deleting list for removed project ${project.id}" }
                services.taskDeleter.delete(it)
            }
        }

        if (response.fullSync) {
            val remoteIds = live.map { it.id }
            for (calendar in stores.caldavDao.findDeletedCalendars(account.uuid!!, remoteIds)) {
                Logger.d(TAG) { "Deleting list absent from full sync: ${calendar.uuid}" }
                services.taskDeleter.delete(calendar)
            }
        }

        for (project in live) {
            val existing = stores.caldavDao.getCalendarByUrl(account.uuid!!, project.id)
            if (existing == null) {
                val calendar = CaldavCalendar(account = account.uuid)
                    .apply { project.applyTo(this) }
                stores.caldavDao.insert(calendar)
                if (project.isInboxProject) {
                    hooks.setDefaultList(CaldavFilter(calendar, account))
                }
            } else {
                val updated = existing.copy().apply { project.applyTo(this) }
                if (updated.name != existing.name ||
                    updated.access != existing.access ||
                    updated.color != existing.color
                ) {
                    project.applyTo(existing)
                    stores.caldavDao.update(existing)
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
                    stores.caldavDao.getTaskByRemoteId(listUuid, item.id)?.let { listOf(it) }.orEmpty()
                } else {
                    lists.values.mapNotNull { stores.caldavDao.getTaskByRemoteId(it.uuid!!, item.id) }
                }
                caldavTasks.map { it.task }
            }
            .flatMap { stores.taskDao.getChildren(it) + it }
            .distinct()
        if (taskIds.isNotEmpty()) {
            Logger.d(TAG) { "Deleting ${taskIds.size} task(s) removed remotely" }
            services.taskDeleter.delete(taskIds)
        }
    }

    private suspend fun updateTask(list: CaldavCalendar, remote: TodoistItem): Boolean {
        val existing = stores.caldavDao.getTaskByRemoteId(list.uuid!!, remote.id)
        val task = existing?.task?.let { stores.taskDao.fetch(it) }
            ?: hooks.createTask().apply { stores.taskDao.createNew(this) }
        val caldavTask = existing
            ?.copy(task = task.id)
            ?: CaldavTask(
                task = task.id,
                calendar = list.uuid,
                remoteId = remote.id,
                obj = "${remote.id}.json",
            )
        val isNew = existing == null
        if (!isNew && stores.dirtyDao.isDirty(caldavTask.id) == true) {
            Logger.d(TAG) { "Ignoring remote update for dirty taskId=${task.id}" }
            return false
        }
        val original = task.copy()
        task.applyRemote(remote)
        task.suppressSync()
        task.suppressRefresh()
        services.taskSaver.save(task, original, dirty = false)
        hooks.vtodoCache.putVtodo(list, caldavTask, json.encodeToString(remote))
        stores.tagDao.applyTags(task, remote.labels)
        caldavTask.remoteParent = remote.parentId ?: ""
        caldavTask.calendar = list.uuid
        stores.caldavDao.insertOrUpdateAndMarkSynced(caldavTask)
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
            val candidates = stores.caldavDao.getTasksByRemoteId(
                list.uuid!!,
                stores.caldavDao.getTopLevelRemoteIds(list.uuid!!).filterNotNull(),
            )
            for (caldavTask in candidates) {
                val remoteId = caldavTask.remoteId ?: continue
                if (remoteId in present) {
                    continue
                }
                if ((stores.dirtyDao.getSyncedVersion(caldavTask.id) ?: 0L) == 0L) {
                    continue
                }
                if (stores.dirtyDao.isDirty(caldavTask.id) == true) {
                    continue
                }
                val task = stores.taskDao.fetch(caldavTask.task) ?: continue
                if (task.isCompleted) {
                    continue
                }
                reap.add(caldavTask.task)
            }
        }
        if (reap.isNotEmpty()) {
            Logger.d(TAG) { "Reaping ${reap.size} task(s) absent from full sync of ${account.uuid}" }
            services.taskDeleter.delete(reap.flatMap { stores.taskDao.getChildren(it) + it }.distinct())
        }
    }

    // endregion

    // region push

    // endregion

    private suspend fun setError(account: CaldavAccount, message: String?) {
        account.error = message
        stores.caldavDao.setError(account.id, message)
        services.refreshBroadcaster.broadcastRefresh()
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

private fun shouldReport(failure: Throwable): Boolean = when (failure) {
    is SocketTimeoutException, is SSLException, is ConnectException,
    is UnknownHostException, is UnauthorizedException, is ServiceUnavailableException,
    is KeyManagementException, is NoSuchAlgorithmException, is IOException -> false
    is HttpException -> failure.code != 401 && failure.code != 403
    else -> true
}
