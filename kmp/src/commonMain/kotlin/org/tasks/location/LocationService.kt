package org.tasks.location

import co.touchlab.kermit.Logger
import org.tasks.data.MergedGeofence
import org.tasks.data.dao.LocationDao
import org.tasks.data.entity.Place
import org.tasks.preferences.AppPreferences

interface LocationService {
    val locationDao: LocationDao
    val appPreferences: AppPreferences

    suspend fun currentLocation(): MapPosition?

    fun addGeofences(geofence: MergedGeofence)

    fun removeGeofences(place: Place)

    fun startBackgroundLocationUpdates(intervalMinutes: Int) {}

    fun stopBackgroundLocationUpdates() {}

    suspend fun updateGeofences(place: Place?) {
        if (place == null) return
        updateGeofenceForPlace(place)
        applyBackgroundLocationUpdates(locationDao.activeGeofenceCount() > 0)
    }

    suspend fun updateGeofences(placeUid: String) =
        updateGeofences(locationDao.getPlace(placeUid))

    suspend fun updateGeofences(taskId: Long) =
        updateGeofences(locationDao.getPlaceForTask(taskId))

    suspend fun registerAllGeofences() {
        val places = locationDao.getPlacesWithGeofences()
        places.forEach { place ->
            updateGeofenceForPlace(place)
        }
        applyBackgroundLocationUpdates(places.isNotEmpty())
    }

    suspend fun refreshBackgroundLocationUpdates() {
        applyBackgroundLocationUpdates(locationDao.activeGeofenceCount() > 0)
    }

    private suspend fun applyBackgroundLocationUpdates(hasGeofences: Boolean) {
        if (hasGeofences) {
            startBackgroundLocationUpdates(appPreferences.locationUpdateIntervalMinutes())
        } else {
            stopBackgroundLocationUpdates()
        }
    }

    private suspend fun updateGeofenceForPlace(place: Place) {
        locationDao
            .getGeofencesByPlace(place.uid!!)?.let {
                Logger.d("LocationService") { "Adding geofence for $it" }
                addGeofences(it)
            }
            ?: run {
                Logger.d("LocationService") { "Removing geofence for $place" }
                removeGeofences(place)
            }
    }
}
