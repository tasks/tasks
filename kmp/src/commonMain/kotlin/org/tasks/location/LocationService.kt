package org.tasks.location

import co.touchlab.kermit.Logger
import org.tasks.data.MergedGeofence
import org.tasks.data.dao.LocationDao
import org.tasks.data.entity.Place

interface LocationService {
    val locationDao: LocationDao

    suspend fun currentLocation(): MapPosition?

    fun addGeofences(geofence: MergedGeofence)

    fun removeGeofences(place: Place)

    suspend fun updateGeofences(place: Place?) {
        if (place == null) return
        locationDao
            .getGeofencesByPlace(place.uid!!)?.let {
                Logger.d("LocationService") { "Adding geofence for $it" }
                addGeofences(it)
            }
            ?: place.let {
                Logger.d("LocationService") { "Removing geofence for $it" }
                removeGeofences(it)
            }
    }

    suspend fun updateGeofences(placeUid: String) =
        updateGeofences(locationDao.getPlace(placeUid))

    suspend fun updateGeofences(taskId: Long) =
        updateGeofences(locationDao.getPlaceForTask(taskId))

    suspend fun registerAllGeofences() =
        locationDao.getPlacesWithGeofences().forEach { updateGeofences(it) }
}
