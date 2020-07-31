package org.tasks.opentasks

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.URGENCY_SPECIFIC_DAY
import com.todoroo.astrid.data.Task.Companion.URGENCY_SPECIFIC_DAY_TIME
import com.todoroo.astrid.data.Task.Companion.sanitizeRRule
import com.todoroo.astrid.helper.UUIDHelper
import com.todoroo.astrid.service.TaskCreator
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.property.Geo
import net.fortuna.ical4j.model.property.RRule
import org.dmfs.tasks.contract.TaskContract.Tasks
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.caldav.CaldavConverter
import org.tasks.caldav.CaldavConverter.toRemote
import org.tasks.caldav.iCalendar
import org.tasks.data.*
import org.tasks.data.OpenTaskDao.Companion.getInt
import org.tasks.data.OpenTaskDao.Companion.getLong
import org.tasks.data.OpenTaskDao.Companion.getString
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils.currentTimeMillis
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
        private val taskCreator: TaskCreator,
        private val taskDao: TaskDao,
        private val firebase: Firebase,
        private val iCalendar: iCalendar,
        private val locationDao: LocationDao,
        private val openTaskDao: OpenTaskDao,
        private val tagDao: TagDao,
        private val tagDataDao: TagDataDao,
        private val inventory: Inventory) {

    private val cr = context.contentResolver

    suspend fun sync() {
        val lists = getLists()
        caldavDao.getAccounts(CaldavAccount.TYPE_OPENTASKS).forEach { account ->
            if (!lists.containsKey(account.uuid)) {
                setError(account, context.getString(R.string.account_not_found))
            } else if (!inventory.hasPro()) {
                setError(account, context.getString(R.string.requires_pro_subscription))
            } else {
                sync(account, lists[account.uuid]!!)
            }
        }
    }

    private suspend fun getLists(): Map<String, List<CaldavCalendar>> =
            openTaskDao.getLists().groupBy { it.account!! }

    private suspend fun sync(account: CaldavAccount, lists: List<CaldavCalendar>) {
        caldavDao
                .findDeletedCalendars(account.uuid!!, lists.mapNotNull { it.url })
                .forEach { taskDeleter.delete(it) }
        lists.forEach {
            val calendar = toLocalCalendar(account.uuid!!, it)
            sync(calendar, it.ctag, it.id)
        }
        setError(account, null)
    }

    private suspend fun toLocalCalendar(account: String, remote: CaldavCalendar): CaldavCalendar {
        val local = caldavDao.getCalendarByUrl(account, remote.url!!) ?: CaldavCalendar().apply {
            uuid = UUIDHelper.newUUID()
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

    private suspend fun sync(calendar: CaldavCalendar, ctag: String?, listId: Long) {
        Timber.d("SYNC $calendar")

        caldavDao.getDeleted(calendar.uuid!!).forEach {
            openTaskDao.delete(listId, it.`object`!!)
            caldavDao.delete(it)
        }

        taskDao
                .getCaldavTasksToPush(calendar.uuid!!)
                .mapNotNull { push(it, listId) }
                .forEach {
                    val tags = tagDataDao.getTagDataForTask(it.task).mapNotNull(TagData::name)
                    openTaskDao.setTags(it, tags)
                    openTaskDao.setRemoteOrder(it)
                    openTaskDao.updateParent(it)
                    it.lastSync = currentTimeMillis()
                    caldavDao.update(it)
                    Timber.d("SENT $it")
                }

        ctag?.let {
            if (ctag == calendar.ctag) {
                Timber.d("UP TO DATE: $calendar")
                return@sync
            }
        }

        val etags = openTaskDao.getEtags(listId)
        etags.forEach { (syncId, etag) ->
            val caldavTask = caldavDao.getTask(calendar.uuid!!, syncId)
            applyChanges(calendar, listId, syncId, etag, caldavTask)
        }
        removeDeleted(calendar.uuid!!, etags.map { it.first })

        calendar.ctag = ctag
        Timber.d("UPDATE $calendar")
        caldavDao.update(calendar)
        caldavDao.updateParents(calendar.uuid!!)
        localBroadcastManager.broadcastRefresh()
    }

    private suspend fun removeDeleted(calendar: String, objects: List<String>) {
        caldavDao
                .getObjects(calendar)
                .subtract(objects)
                .takeIf { it.isNotEmpty() }
                ?.let {
                    Timber.d("DELETED $it")
                    taskDeleter.delete(caldavDao.getTasks(calendar, it.toList()))
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

    private suspend fun push(task: Task, listId: Long): CaldavTask? = withContext(Dispatchers.IO) {
        val caldavTask = caldavDao.getTask(task.id) ?: return@withContext null
        if (task.isDeleted) {
            openTaskDao.delete(listId, caldavTask.`object`!!)
            taskDeleter.delete(task)
            return@withContext null
        }
        val values = ContentValues()
        values.put(Tasks._SYNC_ID, caldavTask.`object`)
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
        } else {
            null
        })
        values.put(Tasks.IS_ALLDAY, if (task.hasDueDate() && !task.hasDueTime()) 1 else 0)
        values.put(Tasks.DUE, when {
            task.hasDueTime() -> newDateTime(task.dueDate).toDateTime().time
            task.hasDueDate() -> Date(task.dueDate).time
            else -> null
        })
        values.put(Tasks.COMPLETED_IS_ALLDAY, 0)
        values.put(Tasks.COMPLETED, if (task.isCompleted) task.completionDate else null)
        values.put(Tasks.STATUS, if (task.isCompleted) Tasks.STATUS_COMPLETED else null)
        values.put(Tasks.PERCENT_COMPLETE, if (task.isCompleted) 100 else null)
        values.put(Tasks.TZ, if (task.hasDueTime() || task.isCompleted) {
            TimeZone.getDefault().id
        } else {
            null
        })
        values.put(Tasks.PARENT_ID, null as Long?)
        val existing = cr.query(
                Tasks.getContentUri(openTaskDao.authority),
                arrayOf(Tasks.PRIORITY),
                "${Tasks.LIST_ID} = $listId AND ${Tasks._SYNC_ID} = '${caldavTask.`object`}'",
                null,
                null)?.use {
            if (!it.moveToFirst()) {
                return@use false
            }
            values.put(Tasks.PRIORITY, toRemote(it.getInt(Tasks.PRIORITY), task.priority))
            true
        } ?: false
        try {
            if (existing) {
                val updated = cr.update(
                        Tasks.getContentUri(openTaskDao.authority),
                        values,
                        "${Tasks.LIST_ID} = $listId AND ${Tasks._SYNC_ID} = '${caldavTask.`object`}'",
                        null)
                if (updated <= 0) {
                    throw Exception("update failed")
                }
            } else {
                values.put(Tasks._UID, caldavTask.remoteId)
                values.put(Tasks.PRIORITY, toRemote(task.priority, task.priority))
                cr.insert(Tasks.getContentUri(openTaskDao.authority), values)
                        ?: throw Exception("insert returned null")
            }
            caldavTask
        } catch (e: Exception) {
            firebase.reportException(e)
            return@withContext null
        }
    }

    private suspend fun applyChanges(
            calendar: CaldavCalendar,
            listId: Long,
            syncId: String,
            etag: String,
            existing: CaldavTask?) {
        cr.query(
                Tasks.getContentUri(openTaskDao.authority),
                null,
                "${Tasks.LIST_ID} = $listId AND ${Tasks._SYNC_ID} = '$syncId'",
                null,
                null)?.use {
            if (!it.moveToFirst()) {
                return
            }
            val task: Task
            val caldavTask: CaldavTask
            if (existing == null) {
                task = taskCreator.createWithValues("")
                taskDao.createNew(task)
                val remoteId = it.getString(Tasks._UID)
                caldavTask = CaldavTask(task.id, calendar.uuid, remoteId, syncId)
            } else {
                task = taskDao.fetch(existing.task)!!
                caldavTask = existing
            }
            if (caldavTask.etag == null || caldavTask.etag != etag) {
                task.title = it.getString(Tasks.TITLE)
                task.priority = CaldavConverter.fromRemote(it.getInt(Tasks.PRIORITY))
                task.completionDate = it.getLong(Tasks.COMPLETED)
                task.notes = it.getString(Tasks.DESCRIPTION)
                task.modificationDate = currentTimeMillis()
                task.creationDate = it.getLong(Tasks.CREATED).toLocal()
                task.setDueDateAdjustingHideUntil(it.getLong(Tasks.DUE).let { due ->
                    when {
                        due == 0L -> 0
                        it.getBoolean(Tasks.IS_ALLDAY) ->
                            Task.createDueDate(URGENCY_SPECIFIC_DAY, due - DateTime(due).offset)
                        else -> Task.createDueDate(URGENCY_SPECIFIC_DAY_TIME, due)
                    }
                })
                iCalendar.setPlace(task.id, it.getString(Tasks.GEO).toGeo())
                task.setRecurrence(it.getString(Tasks.RRULE).toRRule())
                task.suppressSync()
                task.suppressRefresh()
                taskDao.save(task)
                caldavTask.lastSync = DateUtilities.now() + 1000L
                caldavTask.etag = etag
            }
            val tags = openTaskDao.getTags(caldavTask)
            tagDao.applyTags(task, tagDataDao, iCalendar.getTags(tags))
            caldavTask.order = openTaskDao.getRemoteOrder(caldavTask)
            caldavTask.remoteParent = openTaskDao.getParent(it.getLong(Tasks._ID))
            if (caldavTask.id == Task.NO_ID) {
                caldavTask.id = caldavDao.insert(caldavTask)
                Timber.d("NEW $caldavTask")
            } else {
                caldavDao.update(caldavTask)
                Timber.d("UPDATE $caldavTask")
            }
        }
    }

    companion object {
        private fun Location?.toGeoString(): String? = this?.let { "$longitude,$latitude" }

        private fun String?.toGeo(): Geo? =
                this
                        ?.takeIf { it.isNotBlank() }
                        ?.split(",")
                        ?.takeIf {
                            it.size == 2
                                    && it[0].toDoubleOrNull() != null
                                    && it[1].toDoubleOrNull() != null }
                        ?.let { Geo("${it[1]};${it[0]}") }

        private fun String?.toRRule(): RRule? =
                this?.takeIf { it.isNotBlank() }?.let(::RRule)

        private fun Cursor.getBoolean(columnName: String): Boolean =
                getInt(getColumnIndex(columnName)) != 0

        private fun Long.toLocal(): Long =
                DateTime(this).toLocal().millis
    }
}