package org.tasks.injection

import android.app.Notification
import android.app.Service
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.notifications.NotificationManager
import javax.inject.Inject

abstract class InjectingService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    @Inject lateinit var firebase: Firebase

    override fun onCreate() {
        super.onCreate()
        startForeground()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startForeground()
        scope.launch {
            try {
                doWork()
            } catch (e: Exception) {
                firebase.reportException(e)
            } finally {
                done(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun done(startId: Int) {
        scheduleNext()
        stopSelf(startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        job.cancel()
    }

    private fun startForeground() {
        startForeground(notificationId, buildNotification())
    }

    protected abstract val notificationId: Int
    protected abstract val notificationBody: Int

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(
                this, NotificationManager.NOTIFICATION_CHANNEL_MISCELLANEOUS)
                .setSound(null)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_check_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(notificationBody))
                .build()
    }

    protected open fun scheduleNext() {}

    protected abstract suspend fun doWork()
}