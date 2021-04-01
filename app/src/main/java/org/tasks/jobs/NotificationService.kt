package org.tasks.jobs

import android.content.Intent
import android.os.IBinder
import com.todoroo.andlib.utility.AndroidUtilities
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.Notifier
import org.tasks.R
import org.tasks.injection.InjectingService
import org.tasks.preferences.Preferences
import javax.inject.Inject

@AndroidEntryPoint
class NotificationService : InjectingService() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var notifier: Notifier
    @Inject lateinit var notificationQueue: NotificationQueue

    override fun onBind(intent: Intent): IBinder? = null

    override val notificationId = -1

    override val notificationBody = R.string.building_notifications

    @Synchronized
    override suspend fun doWork() {
        AndroidUtilities.assertNotMainThread()
        if (!preferences.isCurrentlyQuietHours) {
            val overdueJobs = notificationQueue.overdueJobs
            if (!notificationQueue.remove(overdueJobs)) {
                throw RuntimeException("Failed to remove jobs from queue")
            }
            notifier.triggerNotifications(overdueJobs.map { it.toNotification() })
        }
    }

    override fun scheduleNext() = notificationQueue.scheduleNext()
}