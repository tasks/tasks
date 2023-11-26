package org.tasks.opentasks

import android.content.Context
import at.bitfire.ical4android.BatchOperation
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.NO_ID
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.qualifiers.ApplicationContext
import org.dmfs.tasks.contract.TaskContract
import org.dmfs.tasks.contract.TaskContract.Tasks
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Constants
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.caldav.iCalendar
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavDao
import org.tasks.data.CaldavTask
import org.tasks.data.MyAndroidTask
import org.tasks.data.OpenTaskDao
import org.tasks.data.OpenTaskDao.Companion.filterActive
import org.tasks.data.OpenTaskDao.Companion.isDavx5
import org.tasks.data.OpenTaskDao.Companion.isDecSync
import org.tasks.data.OpenTaskDao.Companion.isEteSync
import org.tasks.data.OpenTaskDao.Companion.toLocalCalendar
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenTasksSynchronizer @Inject constructor(
        @ApplicationContext private val context: Context,
        private val caldavDao: CaldavDao,
        private val taskDeleter: TaskDeleter,
        private val localBroadcastManager: LocalBroadcastManager,
        private val taskDao: TaskDao,
        private val firebase: Firebase,
        private val iCalendar: iCalendar,
        private val openTaskDao: OpenTaskDao,
        private val inventory: Inventory) {

    suspend fun sync() {
        val lists = openTaskDao.getListsByAccount().filterActive(caldavDao)
        lists.keys
            .filter { caldavDao.getAccountByUuid(it) == null }
            .map {
                CaldavAccount().apply {
                    name = it.split(":")[1]
                    uuid = it
                    accountType = CaldavAccount.TYPE_OPENTASKS
                }
            }
            .onEach { caldavDao.insert(it) }
            .forEach {
                firebase.logEvent(
                    R.string.event_sync_add_account,
                    R.string.param_type to when {
                        it.uuid.isDavx5() -> Constants.SYNC_TYPE_DAVX5
                        it.uuid.isEteSync() -> Constants.SYNC_TYPE_ETESYNC_OT
                        it.uuid.isDecSync() -> Constants.SYNC_TYPE_DECSYNC
                        else -> throw IllegalArgumentException()
                    }
                )
            }
        caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS).forEach { account ->
            val entries = lists[account.uuid!!]
            if (entries == null) {
                taskDeleter.delete(account)
            } else if (!inventory.hasPro) {
                setError(account, context.getString(R.string.requires_pro_subscription))
            } else {
                try {
                    sync(account, entries)
                    setError(account, null)
                } catch (e: Exception) {
                    firebase.reportException(e)
                    setError(account, e.message)
                }
            }
        }
    }

    private suspend fun sync(account: CaldavAccount, lists: List<CaldavCalendar>) {
        val uuid = account.uuid!!
        caldavDao
                .findDeletedCalendars(uuid, lists.mapNotNull { it.url })
                .forEach { taskDeleter.delete(it) }
        lists.forEach {
            val calendar = toLocalCalendar(it)
            if (calendar.access != CaldavCalendar.ACCESS_READ_ONLY) {
                pushChanges(account, calendar, it.id)
            }
            fetchChanges(account, calendar, it.ctag, it.id)
        }
    }

    private suspend fun toLocalCalendar(remote: CaldavCalendar): CaldavCalendar {
        val local = caldavDao.getCalendarByUrl(remote.account!!, remote.url!!)
                ?: remote.toLocalCalendar()
        if (local.id == NO_ID) {
            caldavDao.insert(local)
            Timber.d("Created calendar: $local")
            localBroadcastManager.broadcastRefreshList()
        } else if (
            local.name != remote.name ||
            local.color != remote.color ||
            local.access != remote.access
        ) {
            local.color = remote.color
            local.name = remote.name
            local.access = remote.access
            caldavDao.update(local)
            Timber.d("Updated calendar: $local")
            localBroadcastManager.broadcastRefreshList()
        }
        return local
    }

    private suspend fun pushChanges(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        listId: Long
    ) {
        val moved = caldavDao.getMoved(calendar.uuid!!)
        val (deleted, updated) = taskDao
            .getCaldavTasksToPush(calendar.uuid!!)
            .partition { it.isDeleted }

        (moved + deleted.map(Task::id).let { caldavDao.getTasks(it) })
            .mapNotNull { it.remoteId }
            .map { openTaskDao.delete(listId, it) }
            .let { openTaskDao.batch(it) }
        caldavDao.delete(moved)
        taskDeleter.delete(deleted.map { it.id })

        updated.forEach {
            push(account, it, listId)
        }
    }

    private suspend fun fetchChanges(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        ctag: String?,
        listId: Long
    ) {
        if (calendar.ctag?.equals(ctag) == true) {
            Timber.d("UP TO DATE: $calendar")
            return
        }
        Timber.d("SYNC $calendar")

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
        Timber.d("UPDATE $calendar")
        caldavDao.update(calendar)
        caldavDao.updateParents(calendar.uuid!!)
        localBroadcastManager.broadcastRefresh()
    }

    private suspend fun removeDeleted(calendar: String, uids: List<String>) {
        caldavDao
                .getRemoteIds(calendar)
                .subtract(uids)
                .takeIf { it.isNotEmpty() }
                ?.let {
                    Timber.d("DELETED $it")
                    taskDeleter.delete(caldavDao.getTasksByRemoteId(calendar, it.toList()))
                }
    }

    private suspend fun setError(account: CaldavAccount, message: String?) {
        account.error = message
        caldavDao.update(account)
        localBroadcastManager.broadcastRefreshList()
        if (!message.isNullOrBlank()) {
            Timber.e(message)
        }
    }

    private suspend fun push(account: CaldavAccount, task: Task, listId: Long) {
        val caldavTask = caldavDao.getTask(task.id) ?: return
        val uid = caldavTask.remoteId!!
        val androidTask = openTaskDao.getTask(listId, uid)
                ?: MyAndroidTask(at.bitfire.ical4android.Task())
        iCalendar.toVtodo(account, caldavTask, task, androidTask.task!!)
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

        caldavTask.lastSync = task.modificationDate
        caldavDao.update(caldavTask)
        Timber.d("SENT $caldavTask")
    }

    private suspend fun applyChanges(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        listId: Long,
        uid: String,
        etag: String?,
        existing: CaldavTask?
    ) {
        openTaskDao.getTask(listId, uid)?.let {
            iCalendar.fromVtodo(account, calendar, existing, it.task!!, null, null, etag)
        }
    }

    companion object {
        private val CaldavAccount.isEteSync: Boolean
            get() = uuid?.isEteSync() == true

        private val CaldavAccount.isDecSync: Boolean
            get() = uuid?.isDecSync() == true
    }
}