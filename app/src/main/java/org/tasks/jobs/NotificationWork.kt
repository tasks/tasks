package org.tasks.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.alarms.AlarmService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.Notifier
import org.tasks.analytics.Firebase
import org.tasks.data.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.AlarmDao
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.preferences.Preferences
import timber.log.Timber

@HiltWorker
class NotificationWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    firebase: Firebase,
    private val workManager: WorkManager,
    private val alarmService: AlarmService,
    private val alarmDao: AlarmDao,
    private val preferences: Preferences,
    private val notifier: Notifier,
) : RepeatingWorker(context, workerParams, firebase) {
    private var nextAlarm: Long = 0

    override suspend fun run(): Result {
        if (preferences.isCurrentlyQuietHours) {
            nextAlarm = preferences.adjustForQuietHours(now())
            return Result.success()
        }
        repeat(3) {
            val (overdue, future) = alarmService.getAlarms()
            if (overdue.isNotEmpty()) {
                overdue
                    .sortedBy { it.time }
                    .also { alarms ->
                        alarms
                            .filter { it.type == TYPE_SNOOZE }
                            .map { it.id }
                            .let { alarmDao.deleteByIds(it) }
                    }
                    .map { it.toNotification() }
                    .let { notifier.triggerNotifications(it) }
            } else {
                nextAlarm = future.minOfOrNull { it.time } ?: 0
                Timber.d("nextAlarm=${nextAlarm.toDateTime()}")
                return Result.success()
            }
        }
        firebase.reportException(IllegalStateException("Should have returned already"))
        return Result.failure()
    }

    override suspend fun scheduleNext() {
        if (nextAlarm > 0) {
            workManager.scheduleNotification(preferences.adjustForQuietHours(nextAlarm))
        }
    }
}