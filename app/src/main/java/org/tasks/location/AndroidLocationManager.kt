package org.tasks.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.location.Location
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class AndroidLocationManager @Inject constructor(
        @ApplicationContext private val context: Context,
) : LocationManager {

    private val locationManager
        get() = context.getSystemService(android.location.LocationManager::class.java)

    override val lastKnownLocations: List<Location>
        get() = locationManager.allProviders.mapNotNull {
            locationManager.getLastKnownLocationOrNull(it)
        }

    @SuppressLint("MissingPermission")
    override fun addProximityAlert(
            latitude: Double,
            longitude: Double,
            radius: Float,
            intent: PendingIntent
    ) = locationManager.addProximityAlert(latitude, longitude, radius, -1, intent)

    override fun removeProximityAlert(intent: PendingIntent) =
            locationManager.removeProximityAlert(intent)

    companion object {
        @SuppressLint("MissingPermission")
        private fun android.location.LocationManager.getLastKnownLocationOrNull(provider: String) =
                try {
                    getLastKnownLocation(provider)
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
    }
}