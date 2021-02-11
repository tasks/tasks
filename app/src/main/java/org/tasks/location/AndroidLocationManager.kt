package org.tasks.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class AndroidLocationManager @Inject constructor(
        @ApplicationContext context: Context,
) : LocationManager {

    private val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager

    override val lastKnownLocations: List<Location>
        get() = locationManager.allProviders.mapNotNull {
            locationManager.getLastKnownLocationOrNull(it)
        }

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