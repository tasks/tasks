package org.tasks.data

import androidx.room.Embedded

class MergedGeofence {
    @Embedded lateinit var place: Place
    var arrival = false
    var departure = false
    var radius = 0

    val uid: String?
        get() = place.uid

    val latitude: Double
        get() = place.latitude

    val longitude: Double
        get() = place.longitude

    override fun toString(): String =
            "MergedGeofence(place=$place, arrival=$arrival, departure=$departure, radius=$radius)"
}