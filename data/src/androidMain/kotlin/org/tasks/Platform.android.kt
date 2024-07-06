@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.tasks

import android.location.Location
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.RawValue
import org.tasks.data.BuildConfig
import java.util.Date
import java.util.regex.Pattern
import kotlin.math.abs

actual typealias CommonParcelable = Parcelable

actual typealias CommonRawValue = RawValue

actual typealias CommonIgnoredOnParcel = IgnoredOnParcel

actual val IS_DEBUG = BuildConfig.DEBUG

actual fun Long.printTimestamp(): String = Date(this).toString()

private val pattern = Pattern.compile("(\\d+):(\\d+):(\\d+\\.\\d+)")

actual fun formatCoordinates(coordinates: Double, latitude: Boolean): String {
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
