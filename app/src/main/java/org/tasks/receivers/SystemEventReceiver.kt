package org.tasks.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.LocalBroadcastManager
import org.tasks.location.RegisterGeofencesWork
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SystemEventReceiver : BroadcastReceiver() {

    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive(context, %s)", intent)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                localBroadcastManager.broadcastRefresh()
                RegisterGeofencesWork.enqueue(context)
            }
            Intent.ACTION_USER_PRESENT -> {
                localBroadcastManager.broadcastRefresh()
            }
        }
    }
}
