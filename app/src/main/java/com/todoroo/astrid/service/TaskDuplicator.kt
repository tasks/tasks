package com.todoroo.astrid.service

import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.gcal.GCalHelper
import org.tasks.LocalBroadcastManager
import org.tasks.data.Alarm
import org.tasks.data.AlarmDao
import org.tasks.data.Attachment
import org.tasks.data.CaldavDao
import org.tasks.data.CaldavTask
import org.tasks.data.Geofence
import org.tasks.data.GoogleTaskDao
import org.tasks.data.LocationDao
import org.tasks.data.Tag
import org.tasks.data.TagDao
import org.tasks.data.TagDataDao
import org.tasks.data.TaskAttachmentDao
import org.tasks.db.DbUtils.dbchunk
import org.tasks.preferences.Preferences
import javax.inject.Inject

class TaskDuplicator @Inject constructor(
        private val gcalHelper: GCalHelper,
        private val taskDao: TaskDao,
        private val localBroadcastManager: LocalBroadcastManager,
        private val tagDao: TagDao,
        private val tagDataDao: TagDataDao,
        private val googleTaskDao: GoogleTaskDao,
        private val caldavDao: CaldavDao,
        private val locationDao: LocationDao,
        private val alarmDao: AlarmDao,
        private val preferences: Preferences,
        private val taskAttachmentDao: TaskAttachmentDao,
) {

    suspend fun duplicate(taskIds: List<Long>): List<Task> {
        return taskIds
            .dbchunk()
            .flatMap {
                it.minus(taskDao.getChildren(it).toSet())
            }
            .let { taskDao.fetch(it) }
            .filterNot { it.readOnly }
            .map { clone(it, it.parent) }
            .also { localBroadcastManager.broadcastRefresh() }
    }

    private suspend fun clone(clone: Task, parentId: Long): Task {
        val originalId = clone.id
        with(clone) {
            creationDate = DateUtilities.now()
            modificationDate = DateUtilities.now()
            reminderLast = 0
            completionDate = 0L
            calendarURI = ""
            parent = parentId
            uuid = Task.NO_UUID
            suppressSync()
            suppressRefresh()
        }
        val newId = taskDao.createNew(clone)
        val tags = tagDataDao.getTagDataForTask(originalId)
        if (tags.isNotEmpty()) {
            tagDao.insert(tags.map { Tag(clone, it) })
        }
        val googleTask = googleTaskDao.getByTaskId(originalId)
        val addToTop = preferences.addTasksToTop()
        if (googleTask != null) {
            googleTaskDao.insertAndShift(
                clone,
                CaldavTask(
                    task = clone.id,
                    calendar = googleTask.calendar,
                    remoteId = null
                ),
                addToTop
            )
        }
        val caldavTask = caldavDao.getTask(originalId)
        if (caldavTask != null) {
            val newDavTask = CaldavTask(
                task = clone.id,
                calendar = caldavTask.calendar
            )
            if (parentId != 0L) {
                val remoteParent = caldavDao.getRemoteIdForTask(parentId)
                newDavTask.remoteParent = remoteParent
            }
            caldavDao.insert(clone, newDavTask, addToTop)
        }
        for (g in locationDao.getGeofencesForTask(originalId)) {
            locationDao.insert(
                    Geofence(clone.id, g.place, g.isArrival, g.isDeparture))
        }
        val alarms = alarmDao.getAlarms(originalId)
        if (alarms.isNotEmpty()) {
            alarmDao.insert(alarms.map { Alarm(clone.id, it.time, it.type) })
        }
        gcalHelper.createTaskEventIfEnabled(clone)
        taskDao.save(clone) // TODO: delete me
        taskAttachmentDao
            .getAttachmentsForTask(originalId)
            .map {
                Attachment(
                    task = clone.id,
                    fileId = it.fileId,
                    attachmentUid = it.attachmentUid
                )
            }
            .let { taskAttachmentDao.insert(it) }
        getDirectChildren(originalId).forEach { subtask ->
            clone(subtask, newId)
        }
        return clone
    }

    private suspend fun getDirectChildren(taskId: Long): List<Task> =
        taskDao
            .fetch(taskDao.getChildren(taskId))
            .filter { it.parent == taskId }
}