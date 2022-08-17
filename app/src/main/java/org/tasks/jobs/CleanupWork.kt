package org.tasks.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.timers.TimerPlugin
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import org.tasks.analytics.Firebase
import org.tasks.data.DeletionDao
import org.tasks.data.LocationDao
import org.tasks.data.UserActivityDao
import org.tasks.files.FileHelper
import org.tasks.injection.BaseWorker
import org.tasks.location.GeofenceApi
import org.tasks.notifications.NotificationManager
import timber.log.Timber

@HiltWorker
class CleanupWork @AssistedInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        private val notificationManager: NotificationManager,
        private val geofenceApi: GeofenceApi,
        private val timerPlugin: TimerPlugin,
        private val alarmService: AlarmService,
        private val userActivityDao: UserActivityDao,
        private val locationDao: LocationDao,
        private val deletionDao: DeletionDao) : BaseWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        val tasks = inputData.getLongArray(EXTRA_TASK_IDS)
        if (tasks == null) {
            Timber.e("No task ids provided")
            return Result.failure()
        }
        tasks.forEach { task ->
            runBlocking {
                alarmService.cancelAlarms(task)
            }
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
        timerPlugin.updateNotifications()
        deletionDao.purgeDeleted()
        return Result.success()
    }

    companion object {
        const val EXTRA_TASK_IDS = "extra_task_ids"
    }
}