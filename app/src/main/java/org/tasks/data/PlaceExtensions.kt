package org.tasks.data

import android.content.Context
import android.location.Location
import android.net.Uri
import org.tasks.data.entity.Place
import org.tasks.extensions.Context.openUri
import org.tasks.location.MapPosition
import java.util.regex.Pattern
import kotlin.math.abs

private val pattern = Pattern.compile("(\\d+):(\\d+):(\\d+\\.\\d+)")
private val COORDS = Pattern.compile("^\\d+°\\d+'\\d+\\.\\d+\"[NS] \\d+°\\d+'\\d+\\.\\d+\"[EW]$")

fun Place.open(context: Context?) =
    context?.openUri("geo:$latitude,$longitude?q=${Uri.encode(displayName)}")

val Place.mapPosition: MapPosition
    get() = MapPosition(latitude, longitude)

val Place.displayName: String
    get() {
        if (!name.isNullOrEmpty() && !COORDS.matcher(name!!).matches()) {
            return name!!
        }
        return if (!address.isNullOrEmpty()) {
            address!!
        } else {
            "${formatCoordinate(latitude, true)} ${formatCoordinate(longitude, false)}"
        }
    }

private fun formatCoordinate(coordinates: Double, latitude: Boolean): String {
    val output = Location.convert(abs(coordinates), Location.FORMAT_SECONDS)
    val matcher = pattern.matcher(output)
    return if (matcher.matches()) {
        val direction = if (latitude) {
            if (coordinates > 0) "N" else "S"
        } else {
            if (coordinates > 0) "E" else "W"
        }
        "${matcher.group(1)}°${matcher.group(2)}'${matcher.group(3)}\"$direction"
    } else {
        coordinates.toString()
    }
}
