package org.tasks.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.data.MergedGeofence
import org.tasks.data.Place
import org.tasks.preferences.PermissionChecker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LocationServiceAndroid @Inject constructor(
        @ApplicationContext private val context: Context,
        private val locationManager: LocationManager,
        private val permissionChecker: PermissionChecker,
) : LocationService {

    private var cached: Location? = null

    override suspend fun currentLocation() = if (permissionChecker.canAccessForegroundLocation()) {
        locationManager
                .lastKnownLocations
                .plus(cached)
                .filterNotNull()
                .sortedWith(COMPARATOR)
                .firstOrNull()
                ?.let {
                    cached = it
                    MapPosition(it.latitude, it.longitude)
                }
    } else {
        null
    }

    @SuppressLint("MissingPermission")
    override fun addGeofences(geofence: MergedGeofence) {
        locationManager.addProximityAlert(
                geofence.latitude,
                geofence.longitude,
                geofence.radius.toFloat(),
                createPendingIntent(geofence.place.id)
        )
    }

    override fun removeGeofences(place: Place) {
        locationManager.removeProximityAlert(createPendingIntent(place.id))
    }

    private fun createPendingIntent(place: Long) =
            PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(context, AndroidGeofenceTransitionIntentService.Broadcast::class.java)
                            .setData(Uri.parse("tasks://geofence/$place")),
                    PendingIntent.FLAG_UPDATE_CURRENT
            )

    companion object {
        private val TWO_MINUTES = TimeUnit.MINUTES.toMillis(2)

        internal val COMPARATOR = Comparator<Location> { l1, l2 ->
            val timeDelta = l1.time - l2.time
            val accuracyDelta = l1.accuracy - l2.accuracy
            when {
                timeDelta > TWO_MINUTES -> -1
                timeDelta < -TWO_MINUTES -> 1
                accuracyDelta < 0 -> -1
                accuracyDelta > 0 -> 1
                timeDelta > 0 -> -1
                timeDelta < 0 -> 1
                else -> 0
            }
        }
    }
}