package org.tasks.opentasks

import at.bitfire.ical4android.BatchOperation
import co.touchlab.kermit.Logger
import org.dmfs.tasks.contract.TaskContract
import org.dmfs.tasks.contract.TaskContract.Tasks
import org.jetbrains.compose.resources.getString
import org.tasks.analytics.Reporting
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.Ical4androidTaskAdapter
import org.tasks.caldav.VtodoCache
import org.tasks.caldav.iCalendar
import org.tasks.data.MyAndroidTask
import org.tasks.data.OpenTaskDao
import org.tasks.data.OpenTaskDao.Companion.filterActive
import org.tasks.data.OpenTaskDao.Companion.toLocalCalendar
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.DirtyDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.openTaskProvider
import org.tasks.data.entity.OpenTaskProvider
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.entity.Task.Companion.NO_ID
import org.tasks.service.TaskDeleter
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import java.io.ByteArrayOutputStream
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.requires_pro_subscription

class OpenTasksSynchronizer(
    private val caldavDao: CaldavDao,
    private val taskDeleter: TaskDeleter,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val dirtyDao: DirtyDao,
    private val reporting: Reporting,
    private val iCalendar: iCalendar,
    private val openTaskDao: OpenTaskDao,
    private val vtodoCache: VtodoCache,
) : OpenTasksSyncer {

    override suspend fun sync(hasPro: Boolean) {
        Logger.d("OpenTasksSynchronizer") { "Starting OpenTasks sync..." }
        val lists = openTaskDao.getListsByAccount().filterActive(caldavDao)
        lists.keys
            .filter { caldavDao.getAccountByUuid(it) == null }
            .map {
                CaldavAccount(
                    name = it.split(":")[1],
                    uuid = it,
                    accountType = CaldavAccount.TYPE_OPENTASKS,
                )
            }
            .onEach { caldavDao.insert(it) }
            .forEach {
                reporting.logEvent(
                    "sync_add_account",
                    "type" to (it.uuid.openTaskProvider()?.syncType
                        ?: throw IllegalArgumentException())
                )
            }
        caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS).forEach { account ->
            val entries = lists[account.uuid!!]
            if (entries == null) {
                Logger.d("OpenTasksSynchronizer") { "Removing $account" }
                taskDeleter.delete(account)
            } else if (!hasPro) {
                setError(account, getString(Res.string.requires_pro_subscription))
            } else {
                try {
                    sync(account, entries)
                    if (account.lastSync == 0L) {
                        val taskCount = caldavDao.getTaskCountForAccount(account.uuid!!)
                        val syncType =
                            account.uuid.openTaskProvider()?.syncType ?: "opentasks"
                        reporting.logEvent(
                            "sync_initial_sync_complete",
                            "type" to syncType,
                            "task_count" to taskCount
                        )
                    }
                    account.lastSync = currentTimeMillis()
                    setError(account, null)
                } catch (e: Exception) {
                    reporting.reportException(e)
                    setError(account, e.message)
                }
            }
        }
    }

    private suspend fun sync(account: CaldavAccount, lists: List<CaldavCalendar>) {
        Logger.d("OpenTasksSynchronizer") { "Synchronizing $account" }
        val uuid = account.uuid!!
        caldavDao
                .findDeletedCalendars(uuid, lists.mapNotNull { it.url })
                .forEach {
                    Logger.d("OpenTasksSynchronizer") { "Deleting $it" }
                    taskDeleter.delete(it)
                }
        lists.forEach {
            val calendar = toLocalCalendar(it)
            fetchChanges(account, calendar, it.ctag, it.id)
            if (calendar.access != CaldavCalendar.ACCESS_READ_ONLY) {
                pushLocalChanges(account, calendar, it.id)
            }
        }
    }

    private suspend fun toLocalCalendar(remote: CaldavCalendar): CaldavCalendar {
        val local = caldavDao.getCalendarByUrl(remote.account!!, remote.url!!)
                ?: remote.toLocalCalendar()
        if (local.id == NO_ID) {
            caldavDao.insert(local)
            Logger.d("OpenTasksSynchronizer") { "Created calendar: $local" }
            refreshBroadcaster.broadcastRefresh()
        } else if (
            local.name != remote.name ||
            local.color != remote.color ||
            local.access != remote.access
        ) {
            local.color = remote.color
            local.name = remote.name
            local.access = remote.access
            caldavDao.update(local)
            Logger.d("OpenTasksSynchronizer") { "Updated calendar: $local" }
            refreshBroadcaster.broadcastRefresh()
        }
        return local
    }

    private suspend fun pushLocalChanges(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        listId: Long
    ) {
        val moved = caldavDao.getMoved(calendar.uuid!!)
        val (deleted, updated) = dirtyDao
            .getTasksToPush(calendar.uuid!!)
            .partition { it.task.isDeleted }
        if (moved.isEmpty() && deleted.isEmpty() && updated.isEmpty()) {
            return
        }
        Logger.d("OpenTasksSynchronizer") { "Pushing changes: updated=${updated.size} moved=${moved.size} deleted=${deleted.size}" }
        (moved + deleted.map { it.task.id }
            .let { caldavDao.getTasks(it) })
            .mapNotNull { it.remoteId }
            .takeIf { it.isNotEmpty() }
            ?.map { openTaskDao.delete(listId, it) }
            ?.let {
                Logger.d("OpenTasksSynchronizer") { "Deleting ${it.size} from content provider" }
                openTaskDao.batch(it)
            }
        caldavDao.delete(moved)
        taskDeleter.delete(deleted.map { it.task.id })

        updated.forEach {
            push(account, calendar, it.task, it.caldavTaskId, it.dirtyVersion, listId)
        }
    }

    private suspend fun fetchChanges(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        ctag: String?,
        listId: Long
    ) {
        if (calendar.ctag?.equals(ctag) == true) {
            Logger.d("OpenTasksSynchronizer") { "UP TO DATE: $calendar" }
            return
        }
        Logger.d("OpenTasksSynchronizer") { "SYNC $calendar" }

        val etags = openTaskDao.getEtags(listId)
        etags.forEach { (uid, sync1, version) ->
            val caldavTask = caldavDao.getTaskByRemoteId(calendar.uuid!!, uid)
            val etag = if (account.isEteSync || account.isDecSync) version else sync1
            if (caldavTask?.etag == null || caldavTask.etag != etag) {
                applyChanges(account, calendar, listId, uid, etag, caldavTask)
            }
        }
        removeDeleted(calendar.uuid!!, etags.map { it.first })

        calendar.ctag = ctag
        Logger.d("OpenTasksSynchronizer") { "UPDATE $calendar" }
        caldavDao.update(calendar)
        Logger.d("OpenTasksSynchronizer") { "Updating parents for ${calendar.uuid}" }
        caldavDao.updateParents(calendar.uuid!!)
        refreshBroadcaster.broadcastRefresh()
    }

    private suspend fun removeDeleted(calendar: String, uids: List<String>) {
        caldavDao
                .getRemoteIds(calendar)
                .subtract(uids)
                .takeIf { it.isNotEmpty() }
                ?.let {
                    Logger.d("OpenTasksSynchronizer") { "DELETED $it" }
                    val tasks = caldavDao.getTasksByRemoteId(calendar, it.toList())
                    taskDeleter.delete(tasks.map { it.task })
                }
    }

    private suspend fun setError(account: CaldavAccount, message: String?) {
        account.error = message
        caldavDao.update(account)
        refreshBroadcaster.broadcastRefresh()
        if (!message.isNullOrBlank()) {
            Logger.e("OpenTasksSynchronizer") { message }
        }
    }

    private suspend fun push(account: CaldavAccount, calendar: CaldavCalendar, task: Task, caldavTaskId: Long, dirtyVersion: Long?, listId: Long) {
        val caldavTask = caldavDao.getCaldavTaskById(caldavTaskId) ?: return
        dirtyDao.withDirtyVersion(caldavTaskId, dirtyVersion) {
            val uid = caldavTask.remoteId!!
            if (caldavTask.obj.isNullOrBlank()) {
                caldavTask.obj = "$uid.ics"
            }
            val androidTask = openTaskDao.getTask(listId, uid)
                    ?: MyAndroidTask(at.bitfire.ical4android.Task())
            val adapted = Ical4androidTaskAdapter(androidTask.task!!)
            val data = iCalendar.toVtodo(account, caldavTask, task, adapted)
            val operations = ArrayList<BatchOperation.CpoBuilder>()
            val builder = androidTask.toBuilder(openTaskDao.tasks)
            val idxTask = if (androidTask.isNew) {
                if (account.isEteSync) {
                    builder.withValue(Tasks.SYNC2, uid)
                }
                builder.withValue(Tasks.LIST_ID, listId)
                0
            } else {
                // remove associated rows which are added later again
                operations.add(BatchOperation.CpoBuilder
                        .newDelete(openTaskDao.properties)
                        .withSelection(
                                "${TaskContract.Properties.TASK_ID}=?",
                                arrayOf(androidTask.id.toString())
                        )
                )
                null
            }
            operations.add(builder)
            androidTask.enqueueProperties(openTaskDao.properties, operations, idxTask)

            operations.map { it.build() }.let { openTaskDao.batch(it) }

            vtodoCache.putVtodo(calendar, caldavTask, String(data))
            caldavDao.update(caldavTask)
        }
        Logger.d("OpenTasksSynchronizer") { "SENT $caldavTask" }
    }

    private suspend fun applyChanges(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        listId: Long,
        uid: String,
        etag: String?,
        existing: CaldavTask?
    ) {
        openTaskDao.getTask(listId, uid)?.let { androidTask ->
            val adapted = Ical4androidTaskAdapter(androidTask.task!!)
            val vtodo = ByteArrayOutputStream().let {
                adapted.write(it)
                String(it.toByteArray())
            }
            iCalendar.fromVtodo(account, calendar, existing, adapted, vtodo, "$uid.ics", etag)
        }
    }

    companion object {
        private val CaldavAccount.isEteSync: Boolean
            get() = uuid.openTaskProvider() == OpenTaskProvider.ETESYNC

        private val CaldavAccount.isDecSync: Boolean
            get() = uuid.openTaskProvider() == OpenTaskProvider.DECSYNC
    }
}
