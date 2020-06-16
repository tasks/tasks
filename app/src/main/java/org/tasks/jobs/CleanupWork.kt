package org.tasks.jobs

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.WorkerParameters
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.reminders.ReminderService
import com.todoroo.astrid.timers.TimerPlugin
import org.tasks.analytics.Firebase
import org.tasks.data.DeletionDao
import org.tasks.data.LocationDao
import org.tasks.data.TaskAttachmentDao
import org.tasks.data.UserActivityDao
import org.tasks.files.FileHelper
import org.tasks.injection.InjectingWorker
import org.tasks.location.GeofenceApi
import org.tasks.notifications.NotificationManager
import timber.log.Timber

class CleanupWork @WorkerInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        private val notificationManager: NotificationManager,
        private val geofenceApi: GeofenceApi,
        private val timerPlugin: TimerPlugin,
        private val reminderService: ReminderService,
        private val alarmService: AlarmService,
        private val taskAttachmentDao: TaskAttachmentDao,
        private val userActivityDao: UserActivityDao,
        private val locationDao: LocationDao,
        private val deletionDao: DeletionDao) : InjectingWorker(context, workerParams, firebase) {

    public override fun run(): Result {
        val tasks = inputData.getLongArray(EXTRA_TASK_IDS)
        if (tasks == null) {
            Timber.e("No task ids provided")
            return Result.failure()
        }
        tasks.forEach { task ->
            alarmService.cancelAlarms(task)
            reminderService.cancelReminder(task)
            notificationManager.cancel(task)
            locationDao.getGeofencesForTask(task).forEach {
                locationDao.delete(it)
                geofenceApi.update(it.place!!)
            }
            taskAttachmentDao.getAttachments(task).forEach {
                FileHelper.delete(context, it.parseUri())
                taskAttachmentDao.delete(it)
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