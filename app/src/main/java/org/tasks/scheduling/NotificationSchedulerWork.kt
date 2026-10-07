package org.tasks.scheduling

import android.app.NotificationChannel
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.todoroo.andlib.utility.AndroidUtilities.preS
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.injection.BaseWorker
import org.tasks.jobs.WorkManager
import org.tasks.notifications.NotificationManager

@HiltWorker
class NotificationSchedulerWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    firebase: Firebase,
    private val notificationManager: NotificationManager,
    private val workManager: WorkManager,
) : BaseWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        createNotificationChannels()
        val cancelExistingNotifications = inputData.getBoolean(EXTRA_CANCEL_EXISTING_NOTIFICATIONS, false)
        notificationManager.restoreNotifications(cancelExistingNotifications)
        workManager.triggerNotifications()
        return Result.success()
    }

    private fun createNotificationChannels() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(
                createNotificationChannel(NotificationManager.NOTIFICATION_CHANNEL_DEFAULT, R.string.notifications, true))
        notificationManager.createNotificationChannel(
                createNotificationChannel(NotificationManager.NOTIFICATION_CHANNEL_TASKER, R.string.tasker_locale, true))
        notificationManager.createNotificationChannel(
                createNotificationChannel(
                        NotificationManager.NOTIFICATION_CHANNEL_TIMERS, R.string.TEA_timer_controls, true))
        if (preS()) {
            notificationManager.createNotificationChannel(
                createNotificationChannel(
                    NotificationManager.NOTIFICATION_CHANNEL_MISCELLANEOUS,
                    R.string.miscellaneous,
                    false
                )
            )
        }
    }

    private fun createNotificationChannel(
            channelId: String, nameResId: Int, alert: Boolean): NotificationChannel {
        val channelName = context.getString(nameResId)
        val importance = if (alert) android.app.NotificationManager.IMPORTANCE_HIGH else android.app.NotificationManager.IMPORTANCE_LOW
        val notificationChannel = NotificationChannel(channelId, channelName, importance)
        notificationChannel.enableLights(alert)
        notificationChannel.enableVibration(alert)
        notificationChannel.setShowBadge(alert)
        return notificationChannel
    }

    companion object {
        private const val EXTRA_CANCEL_EXISTING_NOTIFICATIONS = "extra_cancel_existing_notifications"
        private const val TAG_NOTIFICATION_SCHEDULER = "tag_notification_scheduler"

        fun enqueueWork(context: Context, cancelNotifications: Boolean = false) {
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                TAG_NOTIFICATION_SCHEDULER,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequest.Builder(NotificationSchedulerWork::class.java)
                    .setInputData(workDataOf(EXTRA_CANCEL_EXISTING_NOTIFICATIONS to cancelNotifications))
                    .build()
            )
        }
    }
}
