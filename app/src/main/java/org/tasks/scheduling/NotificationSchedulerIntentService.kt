package org.tasks.scheduling

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.content.Context
import android.content.Intent
import android.os.Build
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.andlib.utility.AndroidUtilities.preS
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.injection.InjectingJobIntentService
import org.tasks.jobs.WorkManager
import org.tasks.notifications.NotificationManager
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NotificationSchedulerIntentService : InjectingJobIntentService() {
    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var workManager: WorkManager

    override suspend fun doWork(intent: Intent) {
        Timber.d("onHandleWork(%s)", intent)
        createNotificationChannels()
        val cancelExistingNotifications = intent.getBooleanExtra(EXTRA_CANCEL_EXISTING_NOTIFICATIONS, false)
        notificationManager.restoreNotifications(cancelExistingNotifications)
        workManager.triggerNotifications()
    }

    private fun createNotificationChannels() {
        if (AndroidUtilities.atLeastOreo()) {
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
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
            channelId: String, nameResId: Int, alert: Boolean): NotificationChannel {
        val channelName = context.getString(nameResId)
        val importance = if (alert) android.app.NotificationManager.IMPORTANCE_HIGH else android.app.NotificationManager.IMPORTANCE_LOW
        val notificationChannel = NotificationChannel(channelId, channelName, importance)
        notificationChannel.enableLights(alert)
        notificationChannel.enableVibration(alert)
        notificationChannel.setBypassDnd(alert)
        notificationChannel.setShowBadge(alert)
        return notificationChannel
    }

    companion object {
        private const val EXTRA_CANCEL_EXISTING_NOTIFICATIONS = "extra_cancel_existing_notifications"
        fun enqueueWork(context: Context?, cancelNotifications: Boolean = false) {
            val intent = Intent(context, NotificationSchedulerIntentService::class.java)
            intent.putExtra(EXTRA_CANCEL_EXISTING_NOTIFICATIONS, cancelNotifications)
            enqueueWork(
                    context!!,
                    NotificationSchedulerIntentService::class.java,
                    JOB_ID_NOTIFICATION_SCHEDULER,
                    intent)
        }
    }
}