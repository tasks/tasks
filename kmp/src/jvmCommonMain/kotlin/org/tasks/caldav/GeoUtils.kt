package org.tasks.caldav

import net.fortuna.ical4j.model.property.Geo
import org.tasks.data.Location
import org.tasks.data.entity.Place
import java.math.BigDecimal
import kotlin.math.min

object GeoUtils {
    fun toGeo(location: Location?) = location?.place?.toGeo()

    fun Place.toGeo() = Geo("$latitude;$longitude")

    fun Geo.latitudeLike() = latitude.toLikeString()

    fun Geo.longitudeLike() = longitude.toLikeString()

    fun Double.toLikeString(): String = BigDecimal(toString()).toLikeString()

    fun BigDecimal.toLikeString(): String {
        val string = truncate()
        return if (string.numDecimalPlaces() < PLACE_ACCURACY) string else "${string}%"
    }

    fun Geo.equalish(other: Geo?): Boolean =
            latitude.truncate() == other?.latitude?.truncate()
                    && longitude.truncate() == other.longitude?.truncate()

    private fun String.numDecimalPlaces(): Int {
        val index = indexOf(".")
        return if (index < 0) 0 else length - index - 1
    }

    private fun BigDecimal.truncate(): String {
        val string = stripTrailingZeros().toPlainString()
        val index = string.indexOf(".")
        return if (index < 0) string else string.substring(0.until(min(string.length, index + PLACE_ACCURACY + 1)))
    }

    private const val PLACE_ACCURACY = 4
}