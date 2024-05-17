package org.tasks.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.InterruptionFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executors.newSingleThreadExecutor
import javax.inject.Inject

class ThrottledNotificationManager @Inject constructor(
        @ApplicationContext val context: Context
) {
    private val notificationManagerCompat = NotificationManagerCompat.from(context)
    private val executor = newSingleThreadExecutor()
    private val throttle = Throttle(NOTIFICATIONS_PER_SECOND, executor = executor, tag = "NOTIFY")

    @InterruptionFilter
    val currentInterruptionFilter: Int
        get() = notificationManagerCompat.currentInterruptionFilter

    fun cancel(id: Int) {
        executor.execute {
            notificationManagerCompat.cancel(id)
        }
    }

    fun notify(id: Int, notification: Notification) {
        throttle.run {
            notificationManagerCompat.notify(id, notification)
        }
    }

    fun pause(millis: Long) = throttle.pause(millis)

    companion object {
        private const val NOTIFICATIONS_PER_SECOND = 4
    }
}