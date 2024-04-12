package org.tasks.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.todoroo.astrid.alarms.AlarmService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.tasks.injection.ApplicationScope
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NotificationClearedReceiver : BroadcastReceiver() {
    @Inject lateinit var notificationManager: NotificationManager
    @Inject @ApplicationScope lateinit var scope: CoroutineScope
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var alarmService: AlarmService
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getLongExtra(NotificationManager.EXTRA_NOTIFICATION_ID, -1L)
        Timber.d("cleared $notificationId")
        if (notificationId <= 0L) return
        scope.launch {
            if (!preferences.useSwipeToSnooze()) {
                notificationManager.cancel(notificationId)
                return@launch
            }
            var snoozeTime = preferences.swipeToSnoozeIntervalMS()
            if (snoozeTime == 0L) snoozeTime = 1000L
            alarmService.snooze(snoozeTime, listOf(notificationId))
        }
    }
}

/*

 */