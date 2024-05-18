package org.tasks.jobs

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.todoroo.astrid.alarms.AlarmService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.Notifier
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.notifications.NotificationManager
import timber.log.Timber

@HiltWorker
class NotificationWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    firebase: Firebase,
    private val workManager: WorkManager,
    private val alarmService: AlarmService,
    private val notifier: Notifier,
) : RepeatingWorker(context, workerParams, firebase) {
    private var nextAlarm: Long = 0

    override suspend fun run(): Result {
        nextAlarm = alarmService.triggerAlarms { notifier.triggerNotifications(it) }
        return Result.success()
    }

    override suspend fun scheduleNext() {
        if (nextAlarm > 0) {
            Timber.d("nextAlarm=${nextAlarm.toDateTime()}")
            workManager.scheduleNotification(nextAlarm)
        }
    }

    // Foreground service for Android 11 and below
    override fun getForegroundInfo() = ForegroundInfo(
        -1,
        NotificationCompat.Builder(context, NotificationManager.NOTIFICATION_CHANNEL_MISCELLANEOUS)
            .setSound(null)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.ic_check_white_24dp)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.building_notifications))
            .build()
    )
}