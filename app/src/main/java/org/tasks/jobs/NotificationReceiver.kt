package org.tasks.jobs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver: BroadcastReceiver() {
    @Inject lateinit var workManager: WorkManager

    override fun onReceive(context: Context?, intent: Intent?) {
        workManager.triggerNotifications(expedited = true)
    }
}