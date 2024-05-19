package org.tasks.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Ignore
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import java.io.Serializable

data class Location(
    @Embedded val geofence: Geofence,
    @Embedded val place: Place,
) : Serializable, Parcelable {

    @Ignore
    private constructor(parcel: Parcel): this(
        geofence = parcel.readParcelable(Geofence::class.java.classLoader)!!,
        place = parcel.readParcelable(Place::class.java.classLoader)!!,
    )

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

    val displayName: String
        get() = place.displayName

    val displayAddress: String?
        get() = place.displayAddress

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(geofence, flags)
        dest.writeParcelable(place, flags)
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<Location> = object : Parcelable.Creator<Location> {
            override fun createFromParcel(`in`: Parcel): Location = Location(`in`)

            override fun newArray(size: Int): Array<Location?> = arrayOfNulls(size)
        }
    }
}