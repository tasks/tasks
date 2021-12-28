package org.tasks.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NotificationClearedReceiver : BroadcastReceiver() {
    @Inject lateinit var notificationManager: NotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getLongExtra(NotificationManager.EXTRA_NOTIFICATION_ID, -1L)
        Timber.d("cleared $notificationId")
        GlobalScope.launch {
            notificationManager.cancel(notificationId)
        }
    }
}