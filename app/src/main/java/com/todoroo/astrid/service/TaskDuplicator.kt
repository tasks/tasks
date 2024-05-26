package com.todoroo.astrid.service

import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.gcal.GCalHelper
import org.tasks.LocalBroadcastManager
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.db.DbUtils.dbchunk
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Attachment
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Tag
import org.tasks.data.entity.Task
import org.tasks.data.entity.Task.Companion.NO_ID
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
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

    private suspend fun clone(task: Task, parentId: Long): Task {
        val clone = task.copy(
            id = NO_ID,
            creationDate = currentTimeMillis(),
            modificationDate = currentTimeMillis(),
            reminderLast = 0,
            completionDate = 0L,
            calendarURI = "",
            parent = parentId,
            remoteId = Task.NO_UUID,
        )
        clone.suppressSync()
        clone.suppressRefresh()
        val newId = taskDao.createNew(clone)
        val tags = tagDataDao.getTagDataForTask(task.id)
        if (tags.isNotEmpty()) {
            tagDao.insert(
                tags.map {
                    Tag(
                        task = clone.id,
                        taskUid = clone.uuid,
                        name = it.name,
                        tagUid = it.remoteId
                    )
                }
            )
        }
        val googleTask = googleTaskDao.getByTaskId(task.id)
        val caldavTask = caldavDao.getTask(task.id)
        if (googleTask != null) {
            googleTaskDao.insertAndShift(
                clone,
                CaldavTask(
                    task = clone.id,
                    calendar = googleTask.calendar,
                    remoteId = null
                ),
                preferences.addTasksToTop()
            )
        } else if (caldavTask != null) {
            val newDavTask = CaldavTask(
                task = clone.id,
                calendar = caldavTask.calendar
            )
            if (parentId != 0L) {
                val remoteParent = caldavDao.getRemoteIdForTask(parentId)
                newDavTask.remoteParent = remoteParent
            }
            caldavDao.insert(clone, newDavTask, preferences.addTasksToTop())
        }
        for (g in locationDao.getGeofencesForTask(task.id)) {
            locationDao.insert(
                    Geofence(
                        task = clone.id,
                        place = g.place,
                        isArrival = g.isArrival,
                        isDeparture = g.isDeparture,
                    )
            )
        }
        val alarms = alarmDao.getAlarms(task.id)
        if (alarms.isNotEmpty()) {
            alarmDao.insert(alarms.map { Alarm(task = clone.id, time = it.time, type = it.type) })
        }
        gcalHelper.createTaskEventIfEnabled(clone)
        taskDao.save(clone, null) // TODO: delete me
        taskAttachmentDao
            .getAttachmentsForTask(task.id)
            .map {
                Attachment(
                    task = clone.id,
                    fileId = it.fileId,
                    attachmentUid = it.attachmentUid
                )
            }
            .let { taskAttachmentDao.insert(it) }
        getDirectChildren(task.id).forEach { subtask ->
            clone(subtask, newId)
        }
        return clone
    }

    private suspend fun getDirectChildren(taskId: Long): List<Task> =
        taskDao
            .fetch(taskDao.getChildren(taskId))
            .filter { it.parent == taskId }
}