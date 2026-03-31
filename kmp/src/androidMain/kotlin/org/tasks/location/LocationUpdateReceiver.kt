package org.tasks.location

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import co.touchlab.kermit.Logger

/**
 * No-op receiver for background location updates. The purpose of these updates
 * is to keep the system's location cache fresh so that geofence transitions
 * are detected reliably, even when no other app is requesting location.
 */
class LocationUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logger.v("LocationUpdateReceiver") { "Received background location update" }
    }

    companion object {
        fun pendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, LocationUpdateReceiver::class.java),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                else
                    PendingIntent.FLAG_UPDATE_CURRENT
            )
    }
}
