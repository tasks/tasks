package org.tasks.location

import org.tasks.data.MergedGeofence
import org.tasks.data.Place

interface LocationService {
    suspend fun currentLocation(): MapPosition?

    fun addGeofences(geofence: MergedGeofence)

    fun removeGeofences(place: Place)
}