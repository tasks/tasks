package com.todoroo.astrid.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.tasks.LocalBroadcastManager
import org.tasks.caldav.VtodoCache
import org.tasks.data.dao.DeletionDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.UserActivityDao
import org.tasks.data.db.Database
import org.tasks.data.db.SuspendDbUtils.chunkedMap
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Task
import org.tasks.data.pictureUri
import org.tasks.data.withTransaction
import org.tasks.files.FileHelper
import org.tasks.location.GeofenceApi
import org.tasks.notifications.NotificationManager
import org.tasks.sync.SyncAdapters
import javax.inject.Inject

class TaskDeleter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: Database,
    private val deletionDao: DeletionDao,
    private val taskDao: TaskDao,
    private val localBroadcastManager: LocalBroadcastManager,
    private val syncAdapters: SyncAdapters,
    private val vtodoCache: VtodoCache,
    private val notificationManager: NotificationManager,
    private val geofenceApi: GeofenceApi,
    private val userActivityDao: UserActivityDao,
    private val locationDao: LocationDao,
) {

    suspend fun markDeleted(item: Task) = markDeleted(listOf(item.id))

    suspend fun markDeleted(taskIds: List<Long>): List<Task> = withContext(NonCancellable) {
        val ids = taskIds
            .toSet()
            .plus(taskIds.chunkedMap(taskDao::getChildren))
            .let { taskDao.fetch(it.toList()) }
            .filterNot { it.readOnly }
            .map { it.id }
        database.withTransaction {
            deletionDao.markDeleted(ids)
            cleanup(ids)
        }
        syncAdapters.sync()
        localBroadcastManager.broadcastRefresh()
        taskDao.fetch(ids)
    }

    suspend fun delete(task: Task) = delete(task.id)

    suspend fun delete(task: Long) = delete(listOf(task))

    suspend fun delete(tasks: List<Long>) {
        database.withTransaction {
            deletionDao.delete(tasks)
            cleanup(tasks)
        }
        localBroadcastManager.broadcastRefresh()
    }

    suspend fun delete(list: CaldavCalendar) {
        vtodoCache.delete(list)
        database.withTransaction {
            val tasks = deletionDao.delete(list)
            delete(tasks)
        }
        localBroadcastManager.broadcastRefreshList()
    }

    suspend fun delete(list: CaldavAccount) {
        vtodoCache.delete(list)
        database.withTransaction {
            val tasks = deletionDao.delete(list)
            delete(tasks)
        }
        localBroadcastManager.broadcastRefreshList()
    }

    private suspend fun cleanup(tasks: List<Long>) {
        tasks.forEach { task ->
            notificationManager.cancel(task)
            locationDao.getGeofencesForTask(task).forEach {
                locationDao.delete(it)
                geofenceApi.update(it.place!!)
            }
            userActivityDao.getComments(task).forEach {
                FileHelper.delete(context, it.pictureUri)
                userActivityDao.delete(it)
            }
        }
        notificationManager.updateTimerNotification()
        deletionDao.purgeDeleted()
    }
}
