package org.tasks.location

import android.location.Location
import org.tasks.preferences.PermissionChecker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AndroidLocationProvider @Inject constructor(
        private val locationManager: LocationManager,
        private val permissionChecker: PermissionChecker,
) : LocationProvider {

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