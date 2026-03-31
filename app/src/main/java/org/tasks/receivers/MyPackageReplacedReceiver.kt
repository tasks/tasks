package org.tasks.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.tasks.location.RegisterGeofencesWork
import timber.log.Timber

class MyPackageReplacedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        Timber.d("Package replaced, re-registering geofences")
        RegisterGeofencesWork.enqueue(context)
    }
}
