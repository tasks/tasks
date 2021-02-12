package org.tasks.location

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.data.Place
import org.tasks.data.Place.Companion.newPlace
import javax.inject.Inject

@Suppress("unused")
class AndroidGeocoder @Inject constructor(@ApplicationContext context: Context) : Geocoder {
    private val geocoder = if (android.location.Geocoder.isPresent()) {
        android.location.Geocoder(context)
    } else {
        null
    }

    override suspend fun reverseGeocode(mapPosition: MapPosition): Place =
            withContext(Dispatchers.IO) {
                val addresses = geocoder?.getFromLocation(mapPosition.latitude, mapPosition.longitude, 1) ?: emptyList()
                val place = newPlace(mapPosition)
                if (addresses.isEmpty()) {
                    return@withContext place
                }
                val address = addresses[0]
                if (address.maxAddressLineIndex >= 0) {
                    place.name = address.getAddressLine(0)
                    val builder = StringBuilder(place.name)
                    for (i in 1..address.maxAddressLineIndex) {
                        builder.append(", ").append(address.getAddressLine(i))
                    }
                    place.address = builder.toString()
                }
                if (address.hasLatitude() && address.hasLongitude()) {
                    place.latitude = address.latitude
                    place.longitude = address.longitude
                }
                place.phone = address.phone
                place.url = address.url
                place
            }
}