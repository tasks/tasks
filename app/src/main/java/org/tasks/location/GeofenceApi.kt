package org.tasks.location

import org.tasks.data.LocationDao
import org.tasks.data.Place
import org.tasks.preferences.PermissionChecker
import timber.log.Timber
import javax.inject.Inject

class GeofenceApi @Inject constructor(
        private val permissionChecker: PermissionChecker,
        private val locationDao: LocationDao,
        private val locationService: LocationService
) {
    suspend fun registerAll() = locationDao.getPlacesWithGeofences().forEach { update(it) }

    suspend fun cancelAll() = locationDao.getPlacesWithGeofences().forEach { cancel(it) }

    suspend fun update(taskId: Long) = update(locationDao.getPlaceForTask(taskId))

    suspend fun update(place: String) = update(locationDao.getPlace(place))

    suspend fun update(place: Place?) {
        if (place == null || !permissionChecker.canAccessBackgroundLocation()) {
            return
        }
        locationDao
                .getGeofencesByPlace(place.uid!!)?.let {
                    Timber.d("Adding geofence for %s", it)
                    locationService.addGeofences(it)
                }
                ?: cancel(place)
    }

    private fun cancel(place: Place?) = place?.let {
        Timber.d("Removing geofence for %s", place)
        locationService.removeGeofences(place)
    }
}