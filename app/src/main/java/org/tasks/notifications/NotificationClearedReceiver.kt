package org.tasks.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.tasks.injection.ApplicationScope
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NotificationClearedReceiver : BroadcastReceiver() {
    @Inject lateinit var notificationManager: NotificationManager
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getLongExtra(NotificationManager.EXTRA_NOTIFICATION_ID, -1L)
        Timber.d("cleared $notificationId")
        scope.launch {
            notificationManager.cancel(notificationId)
        }
    }
}