package com.todoroo.astrid.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.tasks.data.dao.DeletionDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.UserActivityDao
import org.tasks.data.pictureUri
import org.tasks.files.FileHelper
import org.tasks.location.LocationService
import org.tasks.notifications.NotificationManager
import org.tasks.service.TaskCleanup
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import javax.inject.Inject

class AndroidCleanup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deletionDao: DeletionDao,
    private val syncAdapters: SyncAdapters,
    private val notificationManager: NotificationManager,
    private val locationService: LocationService,
    private val userActivityDao: UserActivityDao,
    private val locationDao: LocationDao,
) : TaskCleanup {

    override suspend fun cleanup(tasks: List<Long>) {
        if (tasks.isEmpty()) {
            return
        }
        notificationManager.cancel(tasks)
        tasks.forEach { task ->
            locationDao.getGeofencesForTask(task).forEach {
                locationDao.delete(it)
                locationService.updateGeofences(it.place!!)
            }
            userActivityDao.getComments(task).forEach {
                FileHelper.delete(context, it.pictureUri)
                userActivityDao.delete(it)
            }
        }
        coroutineScope {
            launch(Dispatchers.IO) {
                notificationManager.updateTimerNotification()
                deletionDao.purgeDeleted()
            }
        }
    }

    override suspend fun onMarkedDeleted() {
        syncAdapters.sync(SyncSource.TASK_CHANGE)
    }
}
