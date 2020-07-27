package org.tasks.notifications

import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.injection.InjectingBroadcastReceiver
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NotificationClearedReceiver : InjectingBroadcastReceiver() {
    @Inject lateinit var notificationManager: NotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val notificationId = intent.getLongExtra(NotificationManager.EXTRA_NOTIFICATION_ID, -1L)
        Timber.d("cleared $notificationId")
        notificationManager.cancel(notificationId)
    }
}