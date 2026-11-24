package org.tasks.googleapis

import co.touchlab.kermit.Logger
import com.todoroo.astrid.repeats.RepeatTaskHelper
import kotlinx.coroutines.delay
import org.tasks.analytics.Constants
import org.tasks.analytics.Reporting
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.TaskSaver
import org.tasks.data.createDueDate
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.DirtyDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.getDefaultAlarms
import org.tasks.data.setDefaultReminders
import org.tasks.data.setDueDateAdjustingHideUntil
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.preferences.AppPreferences
import org.tasks.resources.getString
import org.tasks.service.TaskCompleter
import org.tasks.service.TaskDeleter
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import kotlinx.io.IOException
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.network_error
import kotlin.math.max
import org.tasks.extensions.truncate

class GoogleTaskSynchronizer(
    private val caldavDao: CaldavDao,
    private val gtasksListService: GtasksListService,
    private val taskDao: TaskDao,
    private val dirtyDao: DirtyDao,
    private val taskSaver: TaskSaver,
    private val reporting: Reporting,
    private val googleTaskDao: GoogleTaskDao,
    private val defaultListProvider: DefaultListProvider,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val taskDeleter: TaskDeleter,
    private val alarmDao: AlarmDao,
    private val appPreferences: AppPreferences,
    private val repeatTaskHelper: RepeatTaskHelper,
    private val taskCompleter: TaskCompleter,
    private val createTask: suspend () -> org.tasks.data.entity.Task,
) {
    suspend fun sync(account: CaldavAccount, api: suspend () -> GoogleTasksApi) {
        Logger.d(TAG) { "$account: start sync" }
        try {
            synchronize(account, api())
        } catch (e: GoogleTasksException) {
            account.error = e.message
            when (e.statusCode) {
                401, 503 -> Logger.e(TAG, e) { e.message.orEmpty() }
                else -> reporting.reportException(e.cause ?: e)
            }
        } catch (e: StaleTaskIdException) {
            account.error = e.message
            reporting.reportException(e)
        } catch (e: IOException) {
            Logger.e(TAG, e) { e.message.orEmpty() }
            account.error = e.message?.takeIf { it.isNotBlank() } ?: getString(Res.string.network_error)
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
                caldavDao.setLastSync(account.id, account.lastSync)
            }
            caldavDao.setError(account.id, account.error)
            refreshBroadcaster.broadcastRefresh()
            Logger.d(TAG) { "$account: end sync" }
        }
    }

    private suspend fun synchronize(account: CaldavAccount, gtasksInvoker: GoogleTasksApi) {
        val gtaskLists: MutableList<GtasksTaskList> = ArrayList()
        var nextPageToken: String? = null
        do {
            val remoteLists = gtasksInvoker.allGtaskLists(nextPageToken) ?: break
            val items = remoteLists.items
            if (items != null) {
                gtaskLists.addAll(items)
            }
            nextPageToken = remoteLists.nextPageToken
        } while (!nextPageToken.isNullOrEmpty())
        val remoteEtags = gtaskLists.associate { it.id to it.etag }
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
        val pushedTo = mutableSetOf<String>()
        var retryTaskId = pushLocalChanges(account, calendars, gtasksInvoker, pushedTo)

        while (retryTaskId != null) {
            if (failedTasks.contains(retryTaskId)) {
                throw StaleTaskIdException(retryTaskId)
            }
            failedTasks.add(retryTaskId)

            Logger.d(TAG) {
                "Retrying push local changes due to stale task ID $retryTaskId (${failedTasks.size} total failed tasks)"
            }

            delay(1000)

            retryTaskId = pushLocalChanges(account, calendars, gtasksInvoker, pushedTo)
        }
        for (list in calendars) {
            val listId = list.uuid!!
            val remoteEtag = remoteEtags[listId]
            val pushed = listId in pushedTo
            val positionsUnchanged = remoteEtag != null && list.ctag == remoteEtag && !pushed
            Logger.d(TAG) {
                "$listId: ctag=${list.ctag} etag=$remoteEtag pushed=$pushed" +
                        " -> ${if (positionsUnchanged) "skip positions" else "fetch positions"}"
            }
            val lastSync = fetchAndApplyRemoteChanges(gtasksInvoker, list) ?: continue
            if (!positionsUnchanged) {
                gtasksInvoker.updatePositions(listId)
            }
            caldavDao.insertOrReplace(list.copy(lastSync = lastSync, ctag = remoteEtag))
        }
        account.error = ""
    }

    private suspend fun GoogleTasksApi.updatePositions(list: String) {
        Logger.d(TAG) { "updatePositions(list=$list)" }
        fetchPositions(list).forEach { task ->
            googleTaskDao.updatePosition(task.id!!, task.parent, task.position!!)
        }
        googleTaskDao.reposition(caldavDao, list)
    }

    private suspend fun GoogleTasksApi.fetchPositions(listId: String): List<GtasksTask> {
        val tasks: MutableList<GtasksTask> = ArrayList()
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

    private suspend fun pushLocalChanges(
        account: CaldavAccount,
        calendars: List<CaldavCalendar>,
        gtasksInvoker: GoogleTasksApi,
        pushedTo: MutableSet<String>,
    ): Long? {
        for (deleted in caldavDao.getMovedByAccount(account.uuid!!)) {
            deleted.remoteId?.let {
                try {
                    gtasksInvoker.deleteGtask(deleted.calendar, it)
                } catch (e: GoogleTasksException) {
                    when (e.statusCode) {
                        400 -> Logger.e(TAG, e) { e.message.orEmpty() }
                        else -> throw e
                    }
                }
            }
            deleted.calendar?.let(pushedTo::add)
            googleTaskDao.delete(deleted)
        }
        val tasks = calendars.flatMap { calendar ->
            dirtyDao.getTasksToPush(calendar.uuid!!).onEach { pushedTo.add(calendar.uuid!!) }
        }
        for (toPush in tasks) {
            try {
                pushTask(toPush.task, toPush.caldavTaskId, toPush.dirtyVersion, gtasksInvoker)
            } catch (e: RetryTaskException) {
                return e.taskId
            } catch (e: HttpNotFoundException) {
                val caldavTask = caldavDao.getCaldavTaskById(toPush.caldavTaskId)
                when {
                    caldavTask == null ->
                        Logger.w(TAG, e) { "Task ${toPush.task.id} has no caldav task" }

                    caldavTask.remoteId.isNullOrEmpty() ->
                        Logger.w(TAG, e) {
                            "Failed to create task ${toPush.task.id}, list ${caldavTask.calendar} not found"
                        }

                    else -> {
                        Logger.w(TAG, e) { "Task ${toPush.task.id} deleted remotely, deleting local copy" }
                        taskDeleter.delete(toPush.task)
                    }
                }
            }
        }
        return null
    }

    private suspend fun pushTask(
        task: org.tasks.data.entity.Task,
        caldavTaskId: Long,
        dirtyVersion: Long?,
        gtasksInvoker: GoogleTasksApi,
    ) {
        val gtasksMetadata = caldavDao.getCaldavTaskById(caldavTaskId) ?: return
        val newlyCreated = gtasksMetadata.remoteId.isNullOrEmpty()
        if (newlyCreated && task.deletionDate > 0) {
            taskDeleter.delete(task.id)
            return
        }
        if (newlyCreated && task.title.isNullOrEmpty()) {
            dirtyVersion?.let { dirtyDao.markPushed(caldavTaskId, it) }
            return
        }
        dirtyDao.withDirtyVersion(caldavTaskId, dirtyVersion) {
            var remoteModel = GtasksTask()
            val remoteId: String?
            val defaultRemoteList = defaultListProvider.getDefaultList()
            var listId =
                if (defaultRemoteList.isGoogleTasks) defaultRemoteList.uuid else DEFAULT_LIST
            if (newlyCreated) { // Create case
                gtasksMetadata.calendar?.takeIf { it.isNotBlank() }?.let {
                    listId = it
                }
            } else { // update case
                remoteId = gtasksMetadata.remoteId
                listId = gtasksMetadata.calendar!!
                remoteModel = remoteModel.copy(id = remoteId)
            }

            // Update the remote model's changed properties
            remoteModel = remoteModel.copy(
                deleted = if (task.isDeleted) true else null,
                title = truncate(task.title, MAX_TITLE_LENGTH),
                notes = truncate(task.notes, MAX_DESCRIPTION_LENGTH),
                due = if (task.hasDueDate()) GtasksDates.unixTimeToGtasksDueDate(task.dueDate) else null,
                completed = if (task.isCompleted) {
                    GtasksDates.unixTimeToGtasksCompletionTime(task.completionDate)
                } else {
                    null
                },
                status = if (task.isCompleted) "completed" else "needsAction",
            )
            if (newlyCreated) {
                val parent = task.parent
                val localParent =
                    if (parent > 0) googleTaskDao.getRemoteId(parent, listId) else null
                val previous = googleTaskDao.getPrevious(
                    listId, if (localParent.isNullOrEmpty()) 0 else parent, task.order ?: 0
                )
                val created: GtasksTask? = try {
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
                    error("Empty response when creating task ${task.id}")
                }
            } else {
                try {
                    if (!task.isDeleted && gtasksMetadata.isMoved) {
                        try {
                            val parent = task.parent
                            val localParent =
                                if (parent > 0) googleTaskDao.getRemoteId(parent, listId) else null
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
                        } catch (e: GoogleTasksException) {
                            if (e.statusCode == 400) {
                                Logger.w(TAG) { "HTTP 400: clearing parent and order" }
                                reporting.reportException(e.cause ?: e)
                                taskDao.setParent(0L, listOf(task.id))
                                taskDao.setOrder(task.id, 0L)
                                googleTaskDao.update(gtasksMetadata.copy(isMoved = false))
                                throw RetryTaskException(task.id)
                            } else {
                                throw e
                            }
                        }
                    }
                    try {
                        gtasksInvoker.updateGtask(listId, remoteModel)
                    } catch (e: GoogleTasksException) {
                        if (e.statusCode == 400 && e.errorMessage == "Invalid task ID") {
                            Logger.w(TAG) { "HTTP 400: Invalid task ID for ${remoteModel.id}, clearing to recreate on next sync" }
                            reporting.reportException(e.cause ?: e)
                            googleTaskDao.update(
                                gtasksMetadata.copy(
                                    remoteId = "",
                                    isMoved = false,
                                )
                            )
                            throw RetryTaskException(task.id)
                        } else {
                            throw e
                        }
                    }
                } catch (e: HttpNotFoundException) {
                    Logger.w(TAG) { "HTTP 404 for $gtasksMetadata" }
                    throw e
                }
            }
            gtasksMetadata.isMoved = false
            write(task, gtasksMetadata)
        }
    }

    private suspend fun fetchAndApplyRemoteChanges(
        gtasksInvoker: GoogleTasksApi,
        list: CaldavCalendar
    ): Long? {
        val listId = list.uuid
        var lastSyncDate = list.lastSync
        val tasks: MutableList<GtasksTask> = ArrayList()
        var nextPageToken: String? = null
        do {
            val taskList: GtasksTasks = try {
                gtasksInvoker.getAllGtasksFromListId(listId, lastSyncDate + 1000L, nextPageToken)
            } catch (e: HttpNotFoundException) {
                reporting.reportException(e)
                return null
            } ?: break

            val items = taskList.items
            if (items != null) {
                tasks.addAll(items)
            }
            nextPageToken = taskList.nextPageToken
        } while (!nextPageToken.isNullOrEmpty())
        tasks.sortWith(PARENTS_FIRST)
        for (gtask in tasks) {
            val remoteId = gtask.id!!
            val inList = googleTaskDao.getByRemoteId(remoteId, listId!!)
            val movedFrom = if (inList != null) {
                null
            } else {
                list.account
                    ?.let { googleTaskDao.getByRemoteIdInAccount(remoteId, it) }
                    ?.also { Logger.d(TAG) { "$remoteId moved from ${it.calendar} to $listId" } }
            }
            var googleTask = inList ?: movedFrom?.copy(calendar = listId)
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
                lastSyncDate = max(lastSyncDate, GtasksDates.parse(it))
            }
            val isDeleted = gtask.deleted
            val isHidden = gtask.hidden
            var recreate = false
            if (isDeleted != null && isDeleted) {
                if (task != null) {
                    if (movedFrom == null) {
                        taskDeleter.delete(task)
                    } else {
                        Logger.d(TAG) {
                            "$remoteId deleted in $listId but lives in ${movedFrom.calendar}, keeping"
                        }
                    }
                }
                continue
            } else if (isHidden != null && isHidden) {
                if (task == null) {
                    continue
                }
                if (task.isRecurring) {
                    recreate = true
                } else if (movedFrom != null) {
                    Logger.d(TAG) {
                        "$remoteId hidden in $listId but lives in ${movedFrom.calendar}, keeping"
                    }
                    continue
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
            task.completionDate = GtasksDates.gtasksCompletedTimeToUnixTime(gtask.completed)
            val dueDate = GtasksDates.gtasksDueTimeToUnixTime(gtask.due)
            mergeDates(createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY, dueDate), task)
            task.notes = getTruncatedValue(task.notes, gtask.notes, MAX_DESCRIPTION_LENGTH)
            if (recreate && task.isCompleted && !original.isCompleted) {
                task.suppressRefresh()
                repeatTaskHelper.handleRepeat(task)
                recreate = !task.isCompleted
                if (recreate) {
                    googleTask.remoteId = ""
                    taskCompleter.setComplete(task, false)
                    val children = taskDao.getChildren(task.id)
                    googleTaskDao.resetRemoteIdsAndMarkDirty(children)
                }
                Logger.d(TAG) {
                    "${if (recreate) "advancing and recreating" else "final occurrence"}: $task"
                }
            }
            val isDirty = googleTask.id > 0 && dirtyDao.isDirty(googleTask.id) == true
            if (!isDirty) {
                gtask.updated?.let { task.modificationDate = GtasksDates.parse(it) }
            }
            if (task.title?.isNotBlank() == true || task.notes?.isNotBlank() == true) {
                write(
                    task = task,
                    googleTask = googleTask,
                    original = original,
                    recreate = recreate,
                    initialSync = list.ctag.isNullOrBlank(),
                )
            }
        }
        return lastSyncDate
    }

    private suspend fun setOrderAndParent(googleTask: CaldavTask, task: GtasksTask, local: org.tasks.data.entity.Task) {
        task.position?.toLongOrNull()?.let { googleTask.remoteOrder = it }
        googleTask.remoteParent = task.parent?.takeIf { it.isNotBlank() }
        local.parent = googleTask.remoteParent?.let { googleTaskDao.getTask(it, googleTask.calendar!!) } ?: 0L
    }

    private suspend fun write(
        task: org.tasks.data.entity.Task,
        googleTask: CaldavTask,
        original: org.tasks.data.entity.Task? = null,
        recreate: Boolean = false,
        initialSync: Boolean = false,
    ) {
        task.suppressSync()
        task.suppressRefresh()
        if (task.isNew) {
            task.setDefaultReminders(appPreferences)
            if (initialSync) {
                task.reminderLast = currentTimeMillis()
            }
            taskDao.createNew(task)
            alarmDao.insert(task.getDefaultAlarms(appPreferences.isDefaultDueTimeEnabled()))
        }
        taskSaver.save(task, original, dirty = false)
        caldavDao.insertOrUpdateAndMarkSynced(googleTask.copy(task = task.id), markDirty = recreate)
    }

    private class RetryTaskException(val taskId: Long) : Exception()

    private class StaleTaskIdException(taskId: Long) : IOException("Invalid Task ID: $taskId")

    companion object {
        private const val TAG = "GoogleTaskSynchronizer"
        private const val DEFAULT_LIST = "@default"
        private const val MAX_TITLE_LENGTH = 1024
        private const val MAX_DESCRIPTION_LENGTH = 8192
        private val PARENTS_FIRST = Comparator { o1: GtasksTask, o2: GtasksTask ->
            if (o1.parent.isNullOrEmpty()) {
                if (o2.parent.isNullOrEmpty()) 0 else -1
            } else {
                if (o2.parent.isNullOrEmpty()) 1 else 0
            }
        }

        fun mergeDates(remoteDueDate: Long, local: org.tasks.data.entity.Task) {
            if (remoteDueDate > 0 && local.hasDueTime()) {
                val oldDate = newDateTime(local.dueDate)
                val newDate = newDateTime(remoteDueDate).withTimeOfDay(
                    oldDate.hourOfDay,
                    oldDate.minuteOfHour,
                    oldDate.secondOfMinute,
                )
                local.setDueDateAdjustingHideUntil(
                    createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY_TIME, newDate.millis))
            } else {
                local.setDueDateAdjustingHideUntil(remoteDueDate)
            }
        }

        fun truncate(string: String?, max: Int): String? = string?.truncate(max)

        fun getTruncatedValue(currentValue: String?, newValue: String?, maxLength: Int): String? =
            if (newValue.isNullOrEmpty()
                || currentValue.isNullOrEmpty()
                || !currentValue.startsWith(newValue)
                || newValue != truncate(currentValue, maxLength)) newValue else currentValue
    }
}
