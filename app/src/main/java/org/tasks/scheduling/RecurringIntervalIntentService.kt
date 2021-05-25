package org.tasks.scheduling

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent
import org.tasks.injection.InjectingJobIntentService
import org.tasks.time.DateTimeUtils
import timber.log.Timber
import javax.inject.Inject

abstract class RecurringIntervalIntentService : InjectingJobIntentService() {
    @Inject lateinit var alarmManager: AlarmManager

    override suspend fun doWork(intent: Intent) {
        val interval = intervalMillis()
        if (interval <= 0) {
            Timber.d("service disabled")
            return
        }
        val now = DateTimeUtils.currentTimeMillis()
        val nextRun = now + interval
        Timber.d("running now [nextRun=${DateTimeUtils.printTimestamp(nextRun)}]")
        run()
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, broadcastClass),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.wakeup(nextRun, pendingIntent)
    }

    abstract val broadcastClass: Class<out BroadcastReceiver>
    abstract suspend fun run()
    abstract fun intervalMillis(): Long
}