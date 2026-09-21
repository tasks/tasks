package org.tasks.caldav

import org.tasks.data.Location
import org.tasks.data.entity.Place
import org.tasks.icalendar.Geo

object GeoUtils {
    fun toGeo(location: Location?) = location?.place?.toGeo()

    fun Place.toGeo() = Geo(latitude, longitude)

    fun Geo.latitudeLike() = latitude.toLikeString()

    fun Geo.longitudeLike() = longitude.toLikeString()

    fun Geo.equalish(other: Geo?): Boolean =
        other != null &&
            latitude.truncateToPlaceAccuracy() == other.latitude.truncateToPlaceAccuracy() &&
            longitude.truncateToPlaceAccuracy() == other.longitude.truncateToPlaceAccuracy()
}
