package org.tasks.backup

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.os.Handler
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.service.TaskCreator.Companion.getDefaultAlarms
import com.todoroo.astrid.service.TaskMover
import com.todoroo.astrid.service.Upgrade_13_2
import com.todoroo.astrid.service.Upgrader
import com.todoroo.astrid.service.Upgrader.Companion.V12_4
import com.todoroo.astrid.service.Upgrader.Companion.V12_8
import com.todoroo.astrid.service.Upgrader.Companion.V6_4
import com.todoroo.astrid.service.Upgrader.Companion.getAndroidColor
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.caldav.VtodoCache
import org.tasks.data.AlarmDao
import org.tasks.data.Attachment
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavDao
import org.tasks.data.CaldavTask
import org.tasks.data.FilterDao
import org.tasks.data.Geofence
import org.tasks.data.LocationDao
import org.tasks.data.Place
import org.tasks.data.Tag
import org.tasks.data.TagDao
import org.tasks.data.TagData
import org.tasks.data.TagDataDao
import org.tasks.data.TaskAttachmentDao
import org.tasks.data.TaskListMetadataDao
import org.tasks.data.UserActivityDao
import org.tasks.db.Migrations.repeatFrom
import org.tasks.db.Migrations.withoutFrom
import org.tasks.filters.FilterCriteriaProvider
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject

class TasksJsonImporter @Inject constructor(
        private val tagDataDao: TagDataDao,
        private val userActivityDao: UserActivityDao,
        private val taskDao: TaskDao,
        private val locationDao: LocationDao,
        private val localBroadcastManager: LocalBroadcastManager,
        private val alarmDao: AlarmDao,
        private val tagDao: TagDao,
        private val filterDao: FilterDao,
        private val taskAttachmentDao: TaskAttachmentDao,
        private val caldavDao: CaldavDao,
        private val preferences: Preferences,
        private val taskMover: TaskMover,
        private val taskListMetadataDao: TaskListMetadataDao,
        private val vtodoCache: VtodoCache,
        private val filterCriteriaProvider: FilterCriteriaProvider,
    ) {

    private val result = ImportResult()

    private fun setProgressMessage(
            handler: Handler, progressDialog: ProgressDialog?, message: String) {
        if (progressDialog == null) {
            return
        }
        handler.post { progressDialog.setMessage(message) }
    }

    suspend fun importTasks(context: Context, backupFile: Uri?, progressDialog: ProgressDialog?): ImportResult {
        val handler = Handler(context.mainLooper)
        val gson = Gson()
        val `is`: InputStream? = try {
            context.contentResolver.openInputStream(backupFile!!)
        } catch (e: FileNotFoundException) {
            throw IllegalStateException(e)
        }
        val reader = InputStreamReader(`is`, TasksJsonExporter.UTF_8)
        val input = gson.fromJson(reader, JsonObject::class.java)
        try {
            val data = input["data"]
            val version = input["version"].asInt
            val backupContainer = gson.fromJson(data, BackupContainer::class.java)
            backupContainer.tags?.forEach { tagData ->
                findTagData(tagData)?.let {
                    return@forEach
                }
                tagData.setColor(themeToColor(context, version, tagData.getColor()!!))
                tagDataDao.createNew(tagData)
            }
            backupContainer.googleTaskAccounts?.forEach { googleTaskAccount ->
                if (caldavDao.getAccount(TYPE_GOOGLE_TASKS, googleTaskAccount.account!!) == null) {
                    caldavDao.insert(
                        CaldavAccount().apply {
                            accountType = TYPE_GOOGLE_TASKS
                            uuid = googleTaskAccount.account
                            name = googleTaskAccount.account
                            username = googleTaskAccount.account
                        }
                    )
                }
            }
            backupContainer.places?.forEach { place ->
                if (locationDao.getByUid(place.uid!!) == null) {
                    locationDao.insert(place)
                }
            }
            backupContainer.googleTaskLists?.forEach { googleTaskList ->
                if (caldavDao.getCalendar(googleTaskList.remoteId!!) == null) {
                    caldavDao.insert(
                        CaldavCalendar(
                            account = googleTaskList.account,
                            uuid = googleTaskList.remoteId,
                            color = themeToColor(context, version, googleTaskList.color ?: 0),

                        )
                    )
                }
            }
            backupContainer.filters
                ?.map {
                    if (version < Upgrade_13_2.VERSION) filterCriteriaProvider.rebuildFilter(it)
                    else it
                }?.forEach { filter ->
                    if (filterDao.getByName(filter.title!!) == null) {
                        filterDao.insert(
                            filter.copy(
                                color = themeToColor(context, version, filter.color ?: 0)
                            )
                        )
                    }
                }
            backupContainer.caldavAccounts?.forEach { account ->
                if (caldavDao.getAccountByUuid(account.uuid!!) == null) {
                    caldavDao.insert(account)
                }
            }
            backupContainer.caldavCalendars?.forEach { calendar ->
                if (caldavDao.getCalendarByUuid(calendar.uuid!!) == null) {
                    caldavDao.insert(
                        calendar.copy(color = themeToColor(context, version, calendar.color))
                    )
                }
            }
            backupContainer.taskListMetadata?.forEach { tlm ->
                val id = tlm.filter.takeIf { it?.isNotBlank() == true } ?: tlm.tagUuid!!
                if (taskListMetadataDao.fetchByTagOrFilter(id) == null) {
                    taskListMetadataDao.insert(tlm)
                }
            }
            backupContainer.taskAttachments?.forEach { attachment ->
                if (taskAttachmentDao.getAttachment(attachment.remoteId) == null) {
                    taskAttachmentDao.insert(attachment)
                }
            }
            backupContainer.tasks?.forEach { backup ->
                result.taskCount++
                setProgressMessage(
                        handler,
                        progressDialog,
                        context.getString(R.string.import_progress_read, result.taskCount))
                val task = backup.task
                taskDao.fetch(task.uuid)
                        ?.let {
                            result.skipCount++
                            return@forEach
                        }
                if (
                    backup.caldavTasks
                        ?.filter { it.deleted == 0L }
                        ?.any {
                            val existing = if (
                                it.`object`.isNullOrBlank() ||
                                it.`object` == "null.ics" // caused by an old bug
                            ) {
                                it.remoteId?.let { remoteId ->
                                    caldavDao.getTaskByRemoteId(it.calendar!!, remoteId)
                                }
                            } else {
                                caldavDao.getTask(it.calendar!!, it.`object`!!)
                            }
                            existing != null
                        } == true
                    ) {
                    result.skipCount++
                    return@forEach
                }
                task.suppressRefresh()
                task.suppressSync()
                taskDao.createNew(task)
                val taskId = task.id
                val taskUuid = task.uuid
                for (alarm in backup.alarms) {
                    alarm.task = taskId
                    alarmDao.insert(alarm)
                }
                if (version < V12_4) {
                    task.defaultReminders(task.ringFlags)
                    alarmDao.insert(task.getDefaultAlarms())
                    task.ringFlags = when {
                        task.isNotifyModeFive -> Task.NOTIFY_MODE_FIVE
                        task.isNotifyModeNonstop -> Task.NOTIFY_MODE_NONSTOP
                        else -> 0
                    }
                    taskDao.save(task)
                }
                if (version < V12_8) {
                    task.repeatFrom = task.recurrence.repeatFrom()
                    task.recurrence = task.recurrence.withoutFrom()
                }
                for (comment in backup.comments) {
                    comment.targetId = taskUuid
                    if (version < V6_4) {
                        comment.convertPictureUri()
                    }
                    userActivityDao.createNew(comment)
                }
                for (googleTask in backup.google) {
                    caldavDao.insert(
                        CaldavTask(
                            task = taskId,
                            calendar = googleTask.listId,
                            remoteId = googleTask.remoteId,
                            remoteOrder = googleTask.remoteOrder,
                            remoteParent = googleTask.remoteParent,
                            lastSync = googleTask.lastSync,
                        )
                    )
                }
                for (location in backup.locations) {
                    val place = Place(
                        longitude = location.longitude,
                        latitude = location.latitude,
                        name = location.name,
                        address = location.address,
                        url = location.url,
                        phone = location.phone,
                    )
                    locationDao.insert(place)
                    locationDao.insert(
                        Geofence(
                            task = taskId,
                            place = place.uid,
                            isArrival = location.arrival,
                            isDeparture = location.departure,
                        )
                    )
                }
                for (tag in backup.tags) {
                    val tagData = findTagData(tag) ?: continue
                    tag.task = taskId
                    tag.tagUid = tagData.remoteId
                    tag.setTaskUid(taskUuid)
                    tagDao.insert(tag)
                }
                backup.geofences?.forEach { geofence ->
                    locationDao.insert(
                        geofence.copy(task = taskId)
                    )
                }
                backup.attachments
                    ?.mapNotNull { taskAttachmentDao.getAttachment(it.attachmentUid) }
                    ?.map {
                        Attachment(
                            task = taskId,
                            fileId = it.id!!,
                            attachmentUid = it.remoteId,
                        )
                    }
                    ?.let { taskAttachmentDao.insert(it) }
                backup.caldavTasks?.forEach { caldavTask ->
                    caldavDao.insert(caldavTask.copy(task = taskId))
                }
                backup.vtodo?.let {
                    val caldavTask =
                        backup.caldavTasks?.firstOrNull { t -> !t.isDeleted() } ?: return@let
                    val caldavCalendar = caldavDao.getCalendar(caldavTask.calendar!!) ?: return@let
                    vtodoCache.putVtodo(caldavCalendar, caldavTask, it)
                }
                result.importCount++
            }
            caldavDao.updateParents()
            val ignoreKeys = ignorePrefs.map { context.getString(it) }
            backupContainer
                    .intPrefs
                    ?.filterNot { (key, _) -> ignoreKeys.contains(key) }
                    ?.forEach { (key, value) -> preferences.setInt(key, value as Int) }
            backupContainer
                    .longPrefs
                    ?.filterNot { (key, _) -> ignoreKeys.contains(key) }
                    ?.forEach { (key, value) -> preferences.setLong(key, value as Long) }
            backupContainer
                    .stringPrefs
                    ?.filterNot { (key, _) -> ignoreKeys.contains(key) }
                    ?.forEach { (key, value) -> preferences.setString(key, value) }
            backupContainer
                    .boolPrefs
                    ?.filterNot { (key, _) -> ignoreKeys.contains(key) }
                    ?.forEach { (key, value) -> preferences.setBoolean(key, value as Boolean) }
            backupContainer
                    .setPrefs
                    ?.filterNot { (key, _) -> ignoreKeys.contains(key) }
                    ?.forEach { (key, value) -> preferences.setStringSet(key, value as HashSet<String>)}
            if (version < Upgrader.V8_2) {
                val themeIndex = preferences.getInt(R.string.p_theme_color, 7)
                preferences.setInt(
                        R.string.p_theme_color,
                        getAndroidColor(context, themeIndex))
            }
            if (version < Upgrader.V9_6) {
                taskMover.migrateLocalTasks()
            }
            reader.close()
            `is`!!.close()
        } catch (e: IOException) {
            Timber.e(e)
        }
        localBroadcastManager.broadcastRefresh()
        return result
    }

    private suspend fun findTagData(tagData: TagData) =
            findTagData(tagData.remoteId!!, tagData.name!!)

    private suspend fun findTagData(tag: Tag) = findTagData(tag.tagUid!!, tag.name!!)

    private suspend fun findTagData(uid: String, name: String): TagData? =
            tagDataDao.getByUuid(uid) ?: tagDataDao.getTagByName(name)

    private fun themeToColor(context: Context, version: Int, color: Int) =
            if (version < Upgrader.V8_2) getAndroidColor(context, color) else color

    class ImportResult {
        var taskCount = 0
        var importCount = 0
        var skipCount = 0
    }

    class LegacyLocation {
        var name: String? = null
        var address: String? = null
        var phone: String? = null
        var url: String? = null
        var latitude = 0.0
        var longitude = 0.0
        var radius = 0
        var arrival = false
        var departure = false
    }

    companion object {
        private val ignorePrefs = intArrayOf(
                R.string.p_current_version,
                R.string.p_backups_android_backup_last
        )
    }
}