package org.tasks.sync.microsoft

import android.content.Context
import at.bitfire.dav4jvm.exception.HttpException
import at.bitfire.dav4jvm.exception.ServiceUnavailableException
import at.bitfire.dav4jvm.exception.UnauthorizedException
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskCreator
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.caldav.VtodoCache
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.ERROR_UNAUTHORIZED
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_WRITE
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_UNKNOWN
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.http.HttpClientFactory
import org.tasks.preferences.Preferences
import org.tasks.sync.microsoft.Error.Companion.toMicrosoftError
import org.tasks.sync.microsoft.MicrosoftConverter.applyRemote
import org.tasks.sync.microsoft.MicrosoftConverter.toRemote
import timber.log.Timber
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.inject.Inject
import javax.net.ssl.SSLException


class MicrosoftSynchronizer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val caldavDao: CaldavDao,
    private val taskDao: TaskDao,
    private val localBroadcastManager: LocalBroadcastManager,
    private val taskDeleter: TaskDeleter,
    private val inventory: Inventory,
    private val firebase: Firebase,
    private val taskCreator: TaskCreator,
    private val httpClientFactory: HttpClientFactory,
    private val tagDao: TagDao,
    private val tagDataDao: TagDataDao,
    private val preferences: Preferences,
    private val vtodoCache: VtodoCache,
) {
    suspend fun sync(account: CaldavAccount) {
        Thread.currentThread().contextClassLoader = context.classLoader

        if (!inventory.hasPro) {
            setError(account, context.getString(R.string.requires_pro_subscription))
            return
        }
        if (isNullOrEmpty(account.password)) {
            setError(account, ERROR_UNAUTHORIZED)
            return
        }
        try {
            synchronize(account)
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
            val message = when(e.code) {
                402, in 500..599 -> e.message
                else -> {
                    firebase.reportException(e)
                    e.message
                }
            }
            setError(account, message)
        } catch (e: Exception) {
            setError(account, e.message)
            firebase.reportException(e)
        }
    }

    private suspend fun synchronize(account: CaldavAccount) {
        Timber.d("Synchronize $account")
        val microsoft = httpClientFactory.getMicrosoftService(account)
        val taskLists = getTaskLists(account, microsoft) ?: return
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
            } else if (local.name != remoteName || local.access != access) {
                remote.applyTo(local)
                caldavDao.update(local)
                localBroadcastManager.broadcastRefreshList()
            }
            if (local.ctag?.isNotBlank() == true) {
                deltaSync(account, local, remote, microsoft)
            } else {
                fullSync(account, local, remote, microsoft)
            }
            pushLocalChanges(local, microsoft)
        }
        setError(account, "")
    }

    private suspend fun pushLocalChanges(
        local: CaldavCalendar,
        microsoft: MicrosoftService,
    ) {
        for (task in caldavDao.getMoved(local.uuid!!)) {
            deleteRemoteResource(microsoft, local, task)
        }
        for (task in taskDao.getCaldavTasksToPush(local.uuid!!)) {
            try {
                pushTask(local, task, microsoft)
            } catch (e: IOException) {
                Timber.e(e)
            }
        }
    }

    private suspend fun deleteRemoteResource(
        microsoft: MicrosoftService,
        list: CaldavCalendar,
        task: CaldavTask,
    ): Boolean {
        val listId = list.uuid
        val taskId = task.remoteId
        val success = when {
            task.lastSync == 0L -> true
            listId.isNullOrBlank() -> false
            taskId.isNullOrBlank() -> false
            else -> {
                microsoft.deleteTask(listId, taskId)
                true
            }
        }
        if (success) {
            vtodoCache.delete(list, task)
            caldavDao.delete(task)
        }
        return success
    }

    private suspend fun pushTask(
        list: CaldavCalendar,
        task: Task,
        microsoft: MicrosoftService,
    ) {
        val caldavTask = caldavDao.getTask(task.id) ?: return
        if (task.isDeleted) {
            if (deleteRemoteResource(microsoft, list, caldavTask)) {
                taskDeleter.delete(taskDao.getChildren(task.id) + task.id)
            }
            return
        }
        val remoteTask = task.toRemote(caldavTask, tagDataDao.getTagDataForTask(task.id))
        try {
            val result: Tasks.Task = if (caldavTask.lastSync == 0L) {
                Timber.d("Uploading new task: $task")
                microsoft.createTask(list.uuid!!, remoteTask)
            } else {
                Timber.d("Updating existing task: $task")
                microsoft.updateTask(list.uuid!!, caldavTask.remoteId!!, remoteTask)
            }
            caldavTask.remoteId = result.id
            caldavTask.obj = "${result.id}.json"
            caldavTask.lastSync = task.modificationDate
            vtodoCache.putVtodo(list, caldavTask, json.encodeToString(result))
            caldavDao.update(caldavTask)
        } catch (e: HttpException) {
            if (e.code == 404) {
                Timber.d("404, deleting $task")
                vtodoCache.delete(list, caldavTask)
                taskDeleter.delete(taskDao.getChildren(task.id) + task.id)
            } else {
                Timber.e(e)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private suspend fun deltaSync(
        account: CaldavAccount,
        list: CaldavCalendar,
        remoteList: TaskLists.TaskList,
        microsoft: MicrosoftService
    ) {
        Timber.d("delta update: $list")
        val tasks = getTasks(account, list, remoteList, microsoft) ?: return
        for (remote in tasks) {
            if (remote.removed == null) {
                updateTask(list, remote)
            } else {
                val caldavTasks = caldavDao.getTasksByRemoteId(list.uuid!!, listOf(remote.id!!))
                vtodoCache.delete(list, caldavTasks)
                val taskIds = caldavTasks.map { it.id }.flatMap { taskDao.getChildren(it) + it }
                taskDeleter.delete(taskIds)
            }
        }
        Timber.d("UPDATE $list")
        caldavDao.update(list)
        localBroadcastManager.broadcastRefresh()
    }

    private suspend fun getTaskLists(
        account: CaldavAccount,
        microsoft: MicrosoftService,
    ): List<TaskLists.TaskList>? {
        val taskLists = ArrayList<TaskLists.TaskList>()
        var nextPageToken: String? = null
        do {
            val response = try {
                if (nextPageToken == null) {
                    microsoft.getLists()
                } else {
                    microsoft.paginateLists(nextPageToken)
                }
            } catch (e: Exception) {
                val error = e.message ?: "Sync failed"
                Timber.e(e)
                setError(account, error)
                return null
            }
            taskLists.addAll(response.value)
            nextPageToken = response.nextPage
            Timber.d("nextPageToken: $nextPageToken")
        } while (nextPageToken?.isNotBlank() == true)
        Timber.d("response: $taskLists")
        return taskLists
    }

    private suspend fun getTasks(
        account: CaldavAccount,
        local: CaldavCalendar,
        remoteList: TaskLists.TaskList,
        microsoft: MicrosoftService,
    ): List<Tasks.Task>? {
        val tasks = ArrayList<Tasks.Task>()
        var nextPageToken: String? = null
        do {
            val response = if (nextPageToken == null) {
                local.ctag
                    ?.let { microsoft.paginateTasks(it) }
                    ?: microsoft.getTasks(remoteList.id!!)
            } else {
                microsoft.paginateTasks(nextPageToken)
            }
            if (!response.status.isSuccess()) {
                response.toMicrosoftError()?.let { error ->
                    when (error.error.code) {
                        "ResourceNotFound",
                        "syncStateNotFound" -> {
                            Timber.e("${local.name}: ${error.error.message}")
                            local.ctag = null
                            caldavDao.update(local)
                            return null
                        }
                        else -> {}
                    }
                }
                Timber.e("failed: ${response.status.value} - ${response.status.description}")
                setError(account, response.status.description)
                return null
            }
            val body = response.body<Tasks>()
            tasks.addAll(body.value)
            nextPageToken = body.nextPage
            body.nextDelta?.let { local.ctag = it}
        } while (nextPageToken?.isNotBlank() == true)
        return tasks
    }

    private suspend fun fullSync(
        account: CaldavAccount,
        list: CaldavCalendar,
        remoteList: TaskLists.TaskList,
        microsoft: MicrosoftService,
    ) {
        Timber.d("full update: $list")
        val tasks = getTasks(account, list, remoteList, microsoft) ?: return
        tasks.forEach { updateTask(list, it) }
        caldavDao
            .getRemoteIds(list.uuid!!)
            .subtract(tasks.map { it.id }.toSet())
            .takeIf { it.isNotEmpty() }
            ?.let {
                Timber.d("DELETED $it")
                val caldavTasks = caldavDao.getTasksByRemoteId(list.uuid!!, it.filterNotNull())
                vtodoCache.delete(list, caldavTasks)
                val taskIds = caldavTasks.map { it.id }.flatMap { taskDao.getChildren(it) + it }
                taskDeleter.delete(taskIds)
            }
        Timber.d("UPDATE $list")
        caldavDao.update(list)
        localBroadcastManager.broadcastRefresh()
    }

    private suspend fun updateTask(list: CaldavCalendar, remote: Tasks.Task) {
        val existing = caldavDao.getTaskByRemoteId(list.uuid!!, remote.id!!)
        val task = existing?.task?.let { taskDao.fetch(it) }
            ?: taskCreator.createWithValues("").apply {
                taskDao.createNew(this)
            }
        val caldavTask =
            existing
                ?.copy(task = task.id)
                ?: CaldavTask(
                    task = task.id,
                    calendar = list.uuid,
                    remoteId = remote.id,
                    obj = "${remote.id}.json"
                )
        val dirty = existing != null && task.modificationDate > existing.lastSync
        task.applyRemote(remote, preferences.defaultPriority)
        val existingSubtasks: List<CaldavTask> = taskDao.getChildren(task.id).let { caldavDao.getTasks(it) }
        val remoteSubtaskIds = remote.checklistItems?.map { it.id } ?: emptyList()
        existingSubtasks
            .filter { it.remoteId?.isNotBlank() == true && !remoteSubtaskIds.contains(it.remoteId) }
            .let { taskDeleter.delete(it.map { it.task }) }
        task.suppressSync()
        task.suppressRefresh()
        taskDao.save(task)
        vtodoCache.putVtodo(list, caldavTask, json.encodeToString(remote))
        tagDao.applyTags(task, tagDataDao, getTags(remote.categories ?: emptyList()))
        remote.checklistItems?.forEach {
            // TODO: handle checklist items
        }
        caldavTask.etag = remote.etag
        if (!dirty) {
            caldavTask.lastSync = task.modificationDate
        }
        if (caldavTask.id == Task.NO_ID) {
            caldavDao.insert(caldavTask)
            Timber.d("NEW $caldavTask")
        } else {
            caldavDao.update(caldavTask)
            Timber.d("UPDATE $caldavTask")
        }
    }

    private suspend fun getTags(categories: List<String>): List<TagData> {
        if (categories.isEmpty()) {
            return emptyList()
        }
        val tags = tagDataDao.getTags(categories).toMutableList()
        val existing = tags.map(TagData::name)
        val toCreate = categories subtract existing.toSet()
        for (name in toCreate) {
            val tag = TagData(name = name)
            tagDataDao.insert(tag)
            tags.add(tag)
        }
        return tags
    }

    private suspend fun setError(account: CaldavAccount, message: String?) {
        account.error = message
        caldavDao.update(account)
        localBroadcastManager.broadcastRefreshList()
        if (!isNullOrEmpty(message)) {
            Timber.e(message)
        }
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }
}
