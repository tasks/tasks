package org.tasks.googleapis

import co.touchlab.kermit.Logger
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.util.DateTime
import com.google.api.services.tasks.model.Task
import com.google.api.services.tasks.model.TaskList
import com.google.api.services.tasks.model.Tasks
import com.todoroo.astrid.repeats.RepeatTaskHelper
import kotlinx.coroutines.delay
import org.tasks.analytics.Constants
import org.tasks.analytics.Reporting
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.TaskSaver
import org.tasks.data.createDueDate
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.getDefaultAlarms
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.preferences.AppPreferences
import org.tasks.service.TaskDeleter
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import java.io.EOFException
import java.io.IOException
import java.net.HttpRetryException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Collections
import javax.net.ssl.SSLException
import kotlin.math.max

class GoogleTaskSynchronizer(
    private val caldavDao: CaldavDao,
    private val gtasksListService: GtasksListService,
    private val taskDao: TaskDao,
    private val taskSaver: TaskSaver,
    private val reporting: Reporting,
    private val googleTaskDao: GoogleTaskDao,
    private val defaultListProvider: DefaultListProvider,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val taskDeleter: TaskDeleter,
    private val alarmDao: AlarmDao,
    private val appPreferences: AppPreferences,
    private val repeatTaskHelper: RepeatTaskHelper,
    private val createTask: suspend () -> org.tasks.data.entity.Task,
) {
    suspend fun sync(account: CaldavAccount, invoker: GtasksInvoker) {
        Logger.d(TAG) { "$account: start sync" }
        try {
            synchronize(account, invoker)
        } catch (e: SocketTimeoutException) {
            Logger.e(TAG, e) { e.message.orEmpty() }
            account.error = e.message
        } catch (e: SSLException) {
            Logger.e(TAG, e) { e.message.orEmpty() }
            account.error = e.message
        } catch (e: SocketException) {
            Logger.e(TAG, e) { e.message.orEmpty() }
            account.error = e.message
        } catch (e: UnknownHostException) {
            Logger.e(TAG, e) { e.message.orEmpty() }
            account.error = e.message
        } catch (e: HttpRetryException) {
            Logger.e(TAG, e) { e.message.orEmpty() }
            account.error = e.message
        } catch (e: EOFException) {
            Logger.e(TAG, e) { e.message.orEmpty() }
            account.error = e.message
        } catch (e: GoogleJsonResponseException) {
            account.error = e.message
            when (e.statusCode) {
                401, 503 -> Logger.e(TAG, e) { e.message.orEmpty() }
                else -> reporting.reportException(e)
            }
        } catch (e: Exception) {
            account.error = e.message
            reporting.reportException(e)
        } finally {
            if (account.error.isNullOrBlank()) {
                if (account.lastSync == 0L) {
                    val taskCount = caldavDao.getTaskCountForAccount(account.uuid!!)
                    reporting.logEvent(
                        "initial_sync_complete",
                        "type" to Constants.SYNC_TYPE_GOOGLE_TASKS,
                        "task_count" to taskCount
                    )
                }
                account.lastSync = currentTimeMillis()
            }
            caldavDao.update(account)
            refreshBroadcaster.broadcastRefresh()
            Logger.d(TAG) { "$account: end sync" }
        }
    }

    @Throws(IOException::class)
    private suspend fun synchronize(account: CaldavAccount, gtasksInvoker: GtasksInvoker) {
        val gtaskLists: MutableList<TaskList> = ArrayList()
        var nextPageToken: String? = null
        var eTag: String? = null
        do {
            val remoteLists = gtasksInvoker.allGtaskLists(nextPageToken) ?: break
            eTag = remoteLists.etag
            val items = remoteLists.items
            if (items != null) {
                gtaskLists.addAll(items)
            }
            nextPageToken = remoteLists.nextPageToken
        } while (!nextPageToken.isNullOrEmpty())
        gtasksListService.updateLists(account, gtaskLists)
        val defaultRemoteList = defaultListProvider.getDefaultList()
        if (defaultRemoteList.isGoogleTasks) {
            val list = caldavDao.getCalendarByUuid(defaultRemoteList.uuid)
            if (list == null) {
                defaultListProvider.clearDefaultList()
            }
        }
        val calendars = caldavDao.getCalendarsByAccount(account.uuid!!)
            .onEach {
                if (it.uuid.isNullOrEmpty()) {
                    reporting.reportException(RuntimeException("Empty remote id"))
                }
            }
            .filterNot { it.uuid.isNullOrEmpty() }
        val failedTasks = mutableSetOf<Long>()
        var retryTaskId = pushLocalChanges(account, calendars, gtasksInvoker)

        while (retryTaskId != null) {
            if (failedTasks.contains(retryTaskId)) {
                throw IOException("Invalid Task ID: $retryTaskId")
            }
            failedTasks.add(retryTaskId)

            Logger.d(TAG) {
                "Retrying push local changes due to stale task ID $retryTaskId (${failedTasks.size} total failed tasks)"
            }

            delay(1000)

            retryTaskId = pushLocalChanges(account, calendars, gtasksInvoker)
        }
        for (list in calendars) {
            fetchAndApplyRemoteChanges(gtasksInvoker, list)
            gtasksInvoker.updatePositions(list.uuid!!)
        }
        account.error = ""
    }

    @Throws(IOException::class)
    private suspend fun GtasksInvoker.updatePositions(list: String) {
        Logger.d(TAG) { "updatePositions(list=$list)" }
        fetchPositions(list).forEach { task ->
            googleTaskDao.updatePosition(task.id, task.parent, task.position)
        }
        googleTaskDao.reposition(caldavDao, list)
    }

    @Throws(IOException::class)
    private suspend fun GtasksInvoker.fetchPositions(listId: String): List<Task> {
        val tasks: MutableList<Task> = ArrayList()
        var nextPageToken: String? = null
        do {
            val taskList = getAllPositions(listId, nextPageToken)
            taskList?.items?.let {
                tasks.addAll(it)
            }
            nextPageToken = taskList?.nextPageToken
        } while (!nextPageToken.isNullOrEmpty())
        return tasks
    }

    @Throws(IOException::class)
    private suspend fun pushLocalChanges(
        account: CaldavAccount,
        calendars: List<CaldavCalendar>,
        gtasksInvoker: GtasksInvoker,
    ): Long? {
        for (deleted in caldavDao.getMovedByAccount(account.uuid!!)) {
            deleted.remoteId?.let {
                try {
                    gtasksInvoker.deleteGtask(deleted.calendar, it)
                } catch (e: GoogleJsonResponseException) {
                    when (e.statusCode) {
                        400 -> Logger.e(TAG, e) { e.message.orEmpty() }
                        else -> throw e
                    }
                }
            }
            googleTaskDao.delete(deleted)
        }
        val tasks = calendars.flatMap { taskDao.getTasksToPush(it.uuid!!) }
        for (task in tasks) {
            val staleTaskId = pushTask(task, account.uuid!!, gtasksInvoker)
            if (staleTaskId != null) {
                return staleTaskId
            }
        }
        return null
    }

    @Throws(IOException::class)
    private suspend fun pushTask(
        task: org.tasks.data.entity.Task,
        account: String,
        gtasksInvoker: GtasksInvoker,
    ): Long? {
        val gtasksMetadata = googleTaskDao.getByTaskId(task.id) ?: return null
        val remoteModel = Task()
        var newlyCreated = false
        val remoteId: String?
        val defaultRemoteList = defaultListProvider.getDefaultList()
        var listId = if (defaultRemoteList.isGoogleTasks) defaultRemoteList.uuid else DEFAULT_LIST
        if (gtasksMetadata.remoteId.isNullOrEmpty()) { // Create case
            gtasksMetadata.calendar?.takeIf { it.isNotBlank() }?.let {
                listId = it
            }
            newlyCreated = true
        } else { // update case
            remoteId = gtasksMetadata.remoteId
            listId = gtasksMetadata.calendar!!
            remoteModel.id = remoteId
        }

        // If task was newly created but without a title, don't sync--we're in the middle of
        // creating a task which may end up being cancelled. Also don't sync new but already
        // deleted tasks
        if (newlyCreated && (task.title.isNullOrEmpty() || task.deletionDate > 0)) {
            return null
        }

        // Update the remote model's changed properties
        if (task.isDeleted) {
            remoteModel.deleted = true
        }
        remoteModel.title = truncate(task.title, MAX_TITLE_LENGTH)
        remoteModel.notes = truncate(task.notes, MAX_DESCRIPTION_LENGTH)
        if (task.hasDueDate()) {
            remoteModel.due = GtasksApiUtilities.unixTimeToGtasksDueDate(task.dueDate)?.toStringRfc3339()
        }
        if (task.isCompleted) {
            remoteModel.completed = GtasksApiUtilities.unixTimeToGtasksCompletionTime(task.completionDate)?.toStringRfc3339()
            remoteModel.status = "completed"
        } else {
            remoteModel.completed = null
            remoteModel.status = "needsAction"
        }
        if (newlyCreated) {
            val parent = task.parent
            val localParent = if (parent > 0) googleTaskDao.getRemoteId(parent, listId) else null
            val previous = googleTaskDao.getPrevious(
                listId, if (localParent.isNullOrEmpty()) 0 else parent, task.order ?: 0)
            val created: Task? = try {
                gtasksInvoker.createGtask(listId, remoteModel, localParent, previous)
            } catch (e: HttpNotFoundException) {
                Logger.e(TAG, e) { "Failed to create task, retry without parent or order" }
                gtasksInvoker.createGtask(listId, remoteModel, null, null)
            }
            if (created != null) {
                gtasksMetadata.remoteId = created.id
                gtasksMetadata.calendar = listId
                setOrderAndParent(gtasksMetadata, created, task)
                Logger.d(TAG) { "Created new task: $gtasksMetadata" }
            } else {
                Logger.e(TAG) { "Empty response when creating task" }
                return null
            }
        } else {
            try {
                if (!task.isDeleted && gtasksMetadata.isMoved) {
                    try {
                        val parent = task.parent
                        val localParent = if (parent > 0) googleTaskDao.getRemoteId(parent, listId) else null
                        val previous = googleTaskDao.getPrevious(
                            listId,
                            if (localParent.isNullOrBlank()) 0 else parent,
                            task.order ?: 0,
                        )
                        gtasksInvoker
                            .moveGtask(
                                listId = listId,
                                taskId = remoteModel.id,
                                parentId = localParent,
                                previousId = previous,
                            )
                            ?.let {
                                setOrderAndParent(
                                    googleTask = gtasksMetadata,
                                    task = it,
                                    local = task,
                                )
                            }
                    } catch (e: GoogleJsonResponseException) {
                        if (e.statusCode == 400) {
                            Logger.w(TAG) { "HTTP 400: clearing parent and order" }
                            reporting.reportException(e)
                            taskDao.setParent(0L, listOf(task.id))
                            taskDao.setOrder(task.id, 0L)
                            googleTaskDao.update(gtasksMetadata.copy(isMoved = false))
                            return task.id
                        } else {
                            throw e
                        }
                    }
                }
                try {
                    gtasksInvoker.updateGtask(listId, remoteModel)
                } catch (e: GoogleJsonResponseException) {
                    if (e.statusCode == 400 && e.details?.message == "Invalid task ID") {
                        Logger.w(TAG) { "HTTP 400: Invalid task ID for ${remoteModel.id}, clearing to recreate on next sync" }
                        reporting.reportException(e)
                        googleTaskDao.update(
                            gtasksMetadata.copy(
                                remoteId = "",
                                isMoved = false,
                            )
                        )
                        return task.id
                    } else {
                        throw e
                    }
                }
            } catch (_: HttpNotFoundException) {
                Logger.w(TAG) { "HTTP 404, deleting $gtasksMetadata" }
                googleTaskDao.delete(gtasksMetadata)
                return null
            }
        }
        gtasksMetadata.isMoved = false
        write(task, gtasksMetadata)
        return null
    }

    @Throws(IOException::class)
    private suspend fun fetchAndApplyRemoteChanges(
        gtasksInvoker: GtasksInvoker,
        list: CaldavCalendar
    ) {
        val listId = list.uuid
        var lastSyncDate = list.lastSync
        val tasks: MutableList<Task> = ArrayList()
        var nextPageToken: String? = null
        do {
            val taskList: Tasks = try {
                gtasksInvoker.getAllGtasksFromListId(listId, lastSyncDate + 1000L, nextPageToken)
            } catch (e: HttpNotFoundException) {
                reporting.reportException(e)
                return
            } ?: break

            val items = taskList.items
            if (items != null) {
                tasks.addAll(items)
            }
            nextPageToken = taskList.nextPageToken
        } while (!nextPageToken.isNullOrEmpty())
        Collections.sort(tasks, PARENTS_FIRST)
        for (gtask in tasks) {
            val remoteId = gtask.id
            var googleTask = googleTaskDao.getByRemoteId(remoteId, listId!!)
            var task: org.tasks.data.entity.Task? = null
            if (googleTask == null) {
                googleTask = CaldavTask(
                    task = 0,
                    calendar = listId,
                    remoteId = null,
                )
            } else if (googleTask.task > 0) {
                task = taskDao.fetch(googleTask.task)
            }
            gtask.updated?.let {
                lastSyncDate = max(lastSyncDate, DateTime(it).value)
            }
            val isDeleted = gtask.deleted
            val isHidden = gtask.hidden
            var recreate = false
            if (isDeleted != null && isDeleted) {
                if (task != null) {
                    taskDeleter.delete(task)
                }
                continue
            } else if (isHidden != null && isHidden) {
                if (task == null) {
                    continue
                }
                if (task.isRecurring) {
                    googleTask.remoteId = ""
                    recreate = true
                } else {
                    taskDeleter.delete(task)
                    continue
                }
            } else {
                if (task == null) {
                    task = createTask()
                }
                setOrderAndParent(googleTask, gtask, task)
                googleTask.remoteId = gtask.id
            }
            val original = task.copy()
            task.title = getTruncatedValue(task.title, gtask.title, MAX_TITLE_LENGTH)
            task.completionDate = GtasksApiUtilities.gtasksCompletedTimeToUnixTime(gtask.completed?.let(::DateTime))
            val dueDate = GtasksApiUtilities.gtasksDueTimeToUnixTime(gtask.due?.let(::DateTime))
            mergeDates(createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY, dueDate), task)
            task.notes = getTruncatedValue(task.notes, gtask.notes, MAX_DESCRIPTION_LENGTH)
            if (recreate && task.isCompleted && !original.isCompleted) {
                task.suppressRefresh()
                repeatTaskHelper.handleRepeat(task)
                recreate = !task.isCompleted
                Logger.d(TAG) {
                    "${if (recreate) "advancing and recreating" else "final occurrence"}: $task"
                }
            }
            if (task.title?.isNotBlank() == true || task.notes?.isNotBlank() == true) {
                write(task, googleTask, original, recreate = recreate)
            }
        }
        caldavDao.insertOrReplace(
            list.copy(
                lastSync = lastSyncDate
            )
        )
    }

    private suspend fun setOrderAndParent(googleTask: CaldavTask, task: Task, local: org.tasks.data.entity.Task) {
        task.position?.toLongOrNull()?.let { googleTask.remoteOrder = it }
        googleTask.remoteParent = task.parent?.takeIf { it.isNotBlank() }
        local.parent = googleTask.remoteParent?.let { googleTaskDao.getTask(it, googleTask.calendar!!) } ?: 0L
    }

    private suspend fun write(
        task: org.tasks.data.entity.Task,
        googleTask: CaldavTask,
        original: org.tasks.data.entity.Task? = null,
        recreate: Boolean = false,
    ) {
        task.suppressSync()
        task.suppressRefresh()
        if (task.isNew) {
            taskDao.createNew(task)
            alarmDao.insert(task.getDefaultAlarms(appPreferences.isDefaultDueTimeEnabled()))
        }
        taskSaver.save(task, original)
        googleTask
            .copy(
                task = task.id,
                lastSync = if (recreate) 0L else task.modificationDate,
            )
            .let {
                if (it.id == 0L) {
                    googleTaskDao.insert(it)
                } else {
                    googleTaskDao.update(it)
                }
            }
    }

    companion object {
        private const val TAG = "GoogleTaskSynchronizer"
        private const val DEFAULT_LIST = "@default"
        private const val MAX_TITLE_LENGTH = 1024
        private const val MAX_DESCRIPTION_LENGTH = 8192
        private val PARENTS_FIRST = Comparator { o1: Task, o2: Task ->
            if (o1.parent.isNullOrEmpty()) {
                if (o2.parent.isNullOrEmpty()) 0 else -1
            } else {
                if (o2.parent.isNullOrEmpty()) 1 else 0
            }
        }

        fun mergeDates(remoteDueDate: Long, local: org.tasks.data.entity.Task) {
            if (remoteDueDate > 0 && local.hasDueTime()) {
                val oldDate = newDateTime(local.dueDate)
                val newDate = newDateTime(remoteDueDate)
                    .withHourOfDay(oldDate.hourOfDay)
                    .withMinuteOfHour(oldDate.minuteOfHour)
                    .withSecondOfMinute(oldDate.secondOfMinute)
                local.setDueDateAdjustingHideUntil(
                    createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY_TIME, newDate.millis))
            } else {
                local.setDueDateAdjustingHideUntil(remoteDueDate)
            }
        }

        fun truncate(string: String?, max: Int): String? =
            if (string == null || string.length <= max) string else string.substring(0, max)

        fun getTruncatedValue(currentValue: String?, newValue: String?, maxLength: Int): String? =
            if (newValue.isNullOrEmpty()
                || newValue.length < maxLength || currentValue.isNullOrEmpty()
                || !currentValue.startsWith(newValue)) newValue else currentValue
    }
}
