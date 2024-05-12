package org.tasks.location

import org.tasks.data.entity.Place

interface Geocoder {
    suspend fun reverseGeocode(mapPosition: MapPosition): Place?
}