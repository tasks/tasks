package org.tasks.receivers

import android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.extensions.Context.canScheduleExactAlarms
import org.tasks.jobs.WorkManager
import org.tasks.scheduling.NotificationSchedulerIntentService
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleExactAlarmsPermissionReceiver : BroadcastReceiver() {

    @Inject lateinit var workManager: WorkManager

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            return
        }

        if (context.canScheduleExactAlarms()) {
            NotificationSchedulerIntentService.enqueueWork(context)
        }
    }
}