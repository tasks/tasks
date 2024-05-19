package org.tasks.data

import androidx.room.Embedded
import org.tasks.data.entity.Place

class MergedGeofence {
    @Embedded lateinit var place: Place
    var arrival = false
    var departure = false

    val uid: String?
        get() = place.uid

    val latitude: Double
        get() = place.latitude

    val longitude: Double
        get() = place.longitude

    val radius: Int
        get() = place.radius

    override fun toString(): String =
            "MergedGeofence(place=$place, arrival=$arrival, departure=$departure)"
}