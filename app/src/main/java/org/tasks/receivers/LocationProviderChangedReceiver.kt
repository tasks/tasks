package org.tasks.receivers

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import org.tasks.location.RegisterGeofencesWork
import timber.log.Timber

class LocationProviderChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != LocationManager.PROVIDERS_CHANGED_ACTION) return
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !locationManager.isLocationEnabled) {
            Timber.d("Location providers changed, but location is disabled")
            return
        }
        Timber.d("Location providers changed, re-registering geofences")
        RegisterGeofencesWork.enqueue(context)
    }

    companion object {
        fun setEnabled(context: Context, enabled: Boolean) {
            Timber.d("LocationProviderChangedReceiver %s", if (enabled) "enabled" else "disabled")
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, LocationProviderChangedReceiver::class.java),
                if (enabled)
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
