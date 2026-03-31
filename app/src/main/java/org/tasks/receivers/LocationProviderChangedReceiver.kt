package org.tasks.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import org.tasks.location.RegisterGeofencesWork
import timber.log.Timber

class LocationProviderChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != LocationManager.PROVIDERS_CHANGED_ACTION) return
        Timber.d("Location providers changed, re-registering geofences")
        RegisterGeofencesWork.enqueue(context)
    }
}
