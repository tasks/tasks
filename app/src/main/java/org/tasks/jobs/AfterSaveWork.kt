package org.tasks.jobs

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.Data
import androidx.work.WorkerParameters
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.SyncFlags
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.reminders.ReminderService
import com.todoroo.astrid.repeats.RepeatTaskHelper
import com.todoroo.astrid.timers.TimerPlugin
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Firebase
import org.tasks.data.CaldavDao
import org.tasks.injection.BaseWorker
import org.tasks.location.GeofenceApi
import org.tasks.notifications.NotificationManager
import org.tasks.scheduling.RefreshScheduler
import org.tasks.sync.SyncAdapters
import timber.log.Timber

class AfterSaveWork @WorkerInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        firebase: Firebase,
        private val repeatTaskHelper: RepeatTaskHelper,
        private val notificationManager: NotificationManager,
        private val geofenceApi: GeofenceApi,
        private val timerPlugin: TimerPlugin,
        private val reminderService: ReminderService,
        private val refreshScheduler: RefreshScheduler,
        private val localBroadcastManager: LocalBroadcastManager,
        private val taskDao: TaskDao,
        private val syncAdapters: SyncAdapters,
        private val workManager: WorkManager,
        private val caldavDao: CaldavDao) : BaseWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        val data = inputData
        val taskId = data.getLong(EXTRA_ID, -1)
        val task = taskDao.fetch(taskId)
        if (task == null) {
            Timber.e("Missing saved task")
            return Result.failure()
        }
        reminderService.scheduleAlarm(task)
        val completionDateModified = task.completionDate != data.getLong(EXTRA_ORIG_COMPLETED, 0)
        val deletionDateModified = task.deletionDate != data.getLong(EXTRA_ORIG_DELETED, 0)
        val justCompleted = completionDateModified && task.isCompleted
        val justDeleted = deletionDateModified && task.isDeleted
        if (justCompleted || justDeleted) {
            notificationManager.cancel(taskId)
        }
        if (completionDateModified || deletionDateModified) {
            geofenceApi.update(taskId)
        }
        if (justCompleted) {
            updateCalendarTitle(task)
            val account = caldavDao.getAccountForTask(taskId)
            if (account == null || !account.isSuppressRepeatingTasks) {
                repeatTaskHelper.handleRepeat(task)
            }
            if (task.timerStart > 0) {
                timerPlugin.stopTimer(task)
            }
        }
        if (data.getBoolean(EXTRA_PUSH_GTASKS, false) && syncAdapters.isGoogleTaskSyncEnabled()
                || data.getBoolean(EXTRA_PUSH_CALDAV, false) && syncAdapters.isCaldavSyncEnabled()) {
            workManager.sync(false)
        }
        refreshScheduler.scheduleRefresh(task)
        if (!data.getBoolean(EXTRA_SUPPRESS_REFRESH, false)) {
            localBroadcastManager.broadcastRefresh()
        }
        return Result.success()
    }

    private fun updateCalendarTitle(task: Task) {
        val calendarUri = task.calendarURI
        if (!isNullOrEmpty(calendarUri)) {
            try {
                // change title of calendar event
                val cr = context.contentResolver
                val values = ContentValues()
                values.put(
                        CalendarContract.Events.TITLE,
                        context.getString(R.string.gcal_completed_title, task.title))
                cr.update(Uri.parse(calendarUri), values, null, null)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    companion object {
        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_ORIG_COMPLETED = "extra_was_completed"
        private const val EXTRA_ORIG_DELETED = "extra_was_deleted"
        private const val EXTRA_PUSH_GTASKS = "extra_push_gtasks"
        private const val EXTRA_PUSH_CALDAV = "extra_push_caldav"
        private const val EXTRA_SUPPRESS_REFRESH = "extra_suppress_refresh"
        fun getInputData(current: Task, original: Task?): Data {
            val suppress = current.checkTransitory(SyncFlags.SUPPRESS_SYNC)
            val forceCaldav = current.checkTransitory(SyncFlags.FORCE_CALDAV_SYNC)
            val builder = Data.Builder()
                    .putLong(EXTRA_ID, current.id)
                    .putBoolean(EXTRA_PUSH_GTASKS, !suppress && !current.googleTaskUpToDate(original))
                    .putBoolean(
                            EXTRA_PUSH_CALDAV, !suppress && (!current.caldavUpToDate(original) || forceCaldav))
                    .putBoolean(EXTRA_SUPPRESS_REFRESH, current.checkTransitory(Task.TRANS_SUPPRESS_REFRESH))
            if (original != null) {
                builder
                        .putLong(EXTRA_ORIG_COMPLETED, original.completionDate)
                        .putLong(EXTRA_ORIG_DELETED, original.deletionDate)
            }
            return builder.build()
        }
    }
}