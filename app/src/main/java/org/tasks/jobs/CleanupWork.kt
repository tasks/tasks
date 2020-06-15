package org.tasks.jobs

import android.content.Context
import androidx.work.WorkerParameters
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.reminders.ReminderService
import com.todoroo.astrid.timers.TimerPlugin
import org.tasks.data.DeletionDao
import org.tasks.data.LocationDao
import org.tasks.data.TaskAttachmentDao
import org.tasks.data.UserActivityDao
import org.tasks.files.FileHelper
import org.tasks.injection.ApplicationComponent
import org.tasks.injection.InjectingWorker
import org.tasks.location.GeofenceApi
import org.tasks.notifications.NotificationManager
import timber.log.Timber
import javax.inject.Inject

class CleanupWork(private val context: Context, workerParams: WorkerParameters) : InjectingWorker(context, workerParams) {

    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var geofenceApi: GeofenceApi
    @Inject lateinit var timerPlugin: TimerPlugin
    @Inject lateinit var reminderService: ReminderService
    @Inject lateinit var alarmService: AlarmService
    @Inject lateinit var taskAttachmentDao: TaskAttachmentDao
    @Inject lateinit var userActivityDao: UserActivityDao
    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var deletionDao: DeletionDao

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

    override fun inject(component: ApplicationComponent) = component.inject(this)

    companion object {
        const val EXTRA_TASK_IDS = "extra_task_ids"
    }
}