package org.tasks.opentasks

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.sanitizeRRule
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.qualifiers.ApplicationContext
import net.fortuna.ical4j.model.property.RRule
import org.dmfs.tasks.contract.TaskContract.Tasks
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Constants
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.caldav.CaldavConverter.toRemote
import org.tasks.caldav.iCalendar
import org.tasks.data.*
import org.tasks.data.CaldavAccount.Companion.openTaskType
import org.tasks.data.OpenTaskDao.Companion.getInt
import org.tasks.data.OpenTaskDao.Companion.isDavx5
import org.tasks.data.OpenTaskDao.Companion.isDecSync
import org.tasks.data.OpenTaskDao.Companion.isEteSync
import org.tasks.data.OpenTaskDao.Companion.newAccounts
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils.startOfDay
import timber.log.Timber
import java.util.*
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
        private val locationDao: LocationDao,
        private val openTaskDao: OpenTaskDao,
        private val tagDataDao: TagDataDao,
        private val inventory: Inventory) {

    private val cr = context.contentResolver

    suspend fun sync() {
        val lists = openTaskDao.getListsByAccount()
        lists.newAccounts(caldavDao)
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
            val calendar = toLocalCalendar(uuid, it)
            sync(account, calendar, it.ctag, it.id)
        }
    }

    private suspend fun toLocalCalendar(account: String, remote: CaldavCalendar): CaldavCalendar {
        val local = caldavDao.getCalendarByUrl(account, remote.url!!)
                ?: CaldavCalendar().apply {
                    uuid = UUID
                            .nameUUIDFromBytes("${account.openTaskType()}${remote.url}".toByteArray())
                            .toString()
                    url = remote.url
                    this.account = account
                    caldavDao.insert(this)
                    Timber.d("Created calendar: $this")
                }
        if (local.name != remote.name || local.color != remote.color) {
            local.color = remote.color
            local.name = remote.name
            caldavDao.update(local)
            Timber.d("Updated calendar: $local")
            localBroadcastManager.broadcastRefreshList()
        }
        return local
    }

    private suspend fun sync(
            account: CaldavAccount,
            calendar: CaldavCalendar,
            ctag: String?,
            listId: Long
    ) {
        Timber.d("SYNC $calendar")
        val isEteSync = account.uuid?.isEteSync() == true

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

        val operations = updated.mapNotNull { toOperation(it, listId, isEteSync) }
        val caldavTasks = operations.map { it.first }
        openTaskDao.batch(operations.map { it.second })
        openTaskDao.batch(caldavTasks.flatMap {
            val id = openTaskDao.getId(listId, it.remoteId)
                    ?: return@flatMap emptyList<ContentProviderOperation>()
            val tags = tagDataDao.getTagDataForTask(it.task).mapNotNull(TagData::name)
            val parent = openTaskDao.getId(listId, it.remoteParent)
            openTaskDao.setTags(id, tags)
                    .plus(openTaskDao.setRemoteOrder(id, it))
                    .plus(openTaskDao.updateParent(id, parent))
        })

        caldavTasks
                .takeIf { it.isNotEmpty() }
                ?.let {
                    caldavDao.update(it) // apply task modification date
                    Timber.d("SENT ${it.joinToString("\n")}")
                }

        ctag?.let {
            if (ctag == calendar.ctag) {
                Timber.d("UP TO DATE: $calendar")
                return@sync
            }
        }

        val etags = openTaskDao.getEtags(listId)
        etags.forEach { (uid, sync1, version) ->
            val caldavTask = caldavDao.getTaskByRemoteId(calendar.uuid!!, uid)
            val etag = if (isEteSync) version else sync1
            if (caldavTask?.etag == null || caldavTask.etag != etag) {
                applyChanges(calendar, listId, uid, etag, caldavTask)
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

    private suspend fun toOperation(
            task: Task,
            listId: Long,
            isEteSync: Boolean
    ): Pair<CaldavTask, ContentProviderOperation>? {
        val caldavTask = caldavDao.getTask(task.id) ?: return null
        caldavTask.lastSync = task.modificationDate
        val values = ContentValues()
        values.put(Tasks.LIST_ID, listId)
        values.put(Tasks.TITLE, task.title)
        values.put(Tasks.DESCRIPTION, task.notes)
        values.put(Tasks.GEO, locationDao.getGeofences(task.id).toGeoString())
        values.put(Tasks.RRULE, if (task.isRecurring) {
            val rrule = RRule(task.getRecurrenceWithoutFrom()!!.replace("RRULE:", ""))
            if (task.repeatUntil > 0) {
                rrule.recur.until = DateTime(task.repeatUntil).toUTC().toDateTime()
            }
            RRule(rrule.value.sanitizeRRule()).value
        } else null)
        val allDay = !task.hasDueTime() && !task.hasStartTime()
        values.put(Tasks.IS_ALLDAY, if (allDay) 1 else 0)
        values.put(Tasks.DUE, when {
            task.hasDueTime() -> task.dueDate
            task.hasDueDate() -> task.dueDate.startOfDay()
            else -> null
        })
        values.put(Tasks.DTSTART, when {
            task.hasStartTime() -> task.hideUntil
            task.hasStartDate() -> task.hideUntil.startOfDay()
            else -> null
        })
        values.put(Tasks.COMPLETED_IS_ALLDAY, 0)
        values.put(Tasks.COMPLETED, if (task.isCompleted) task.completionDate else null)
        values.put(Tasks.STATUS, if (task.isCompleted) Tasks.STATUS_COMPLETED else null)
        values.put(Tasks.PERCENT_COMPLETE, if (task.isCompleted) 100 else null)
        if (!allDay || task.isCompleted) {
            values.put(Tasks.TZ, TimeZone.getDefault().id)
        }
        values.put(Tasks.PARENT_ID, null as Long?)
        val existing = cr.query(
                Tasks.getContentUri(openTaskDao.authority),
                arrayOf(Tasks.PRIORITY),
                "${Tasks.LIST_ID} = $listId AND ${Tasks._UID} = '${caldavTask.remoteId}'",
                null,
                null)?.use {
            if (!it.moveToFirst()) {
                return@use false
            }
            values.put(Tasks.PRIORITY, toRemote(it.getInt(Tasks.PRIORITY), task.priority))
            true
        } ?: false
        val operation = try {
            if (existing) {
                openTaskDao.update(listId, caldavTask.remoteId!!, values)
            } else {
                if (isEteSync) {
                    values.put(Tasks.SYNC2, caldavTask.remoteId)
                }
                values.put(Tasks._UID, caldavTask.remoteId)
                values.put(Tasks.PRIORITY, toRemote(task.priority, task.priority))
                openTaskDao.insert(values)
            }
        } catch (e: Exception) {
            firebase.reportException(e)
            null
        }
        return operation?.let { Pair(caldavTask, it) }
    }

    private suspend fun applyChanges(
            calendar: CaldavCalendar,
            listId: Long,
            uid: String,
            etag: String?,
            existing: CaldavTask?
    ) {
        openTaskDao.getTask(listId, uid)?.let {
            iCalendar.fromVtodo(calendar, existing, it, null, null, etag)
        }
    }

    companion object {
        private fun Location?.toGeoString(): String? = this?.let { "$longitude,$latitude" }
    }
}