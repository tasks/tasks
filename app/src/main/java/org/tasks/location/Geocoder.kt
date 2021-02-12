package org.tasks.location

import org.tasks.data.Place

interface Geocoder {
    suspend fun reverseGeocode(mapPosition: MapPosition): Place
}