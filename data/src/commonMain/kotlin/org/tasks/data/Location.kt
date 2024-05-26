package org.tasks.data

import androidx.room.Embedded
import org.tasks.CommonParcelable
import org.tasks.CommonParcelize
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import java.io.Serializable

@CommonParcelize
data class Location(
    @Embedded val geofence: Geofence,
    @Embedded val place: Place,
) : Serializable, CommonParcelable {

    val task: Long
        get() = geofence.task

    val latitude: Double
        get() = place.latitude

    val longitude: Double
        get() = place.longitude

    val radius: Int
        get() = place.radius

    val phone: String?
        get() = place.phone

    val url: String?
        get() = place.url

    val isArrival: Boolean
        get() = geofence.isArrival

    val isDeparture: Boolean
        get() = geofence.isDeparture

    val displayAddress: String?
        get() = place.displayAddress
}