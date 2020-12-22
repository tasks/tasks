package org.tasks.data

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Ignore
import java.io.Serializable

class Location : Serializable, Parcelable {
    @Embedded lateinit var geofence: Geofence
    @Embedded lateinit var place: Place

    constructor()

    @Ignore
    constructor(geofence: Geofence, place: Place) {
        this.geofence = geofence
        this.place = place
    }

    @Ignore
    private constructor(parcel: Parcel) {
        geofence = parcel.readParcelable(Geofence::class.java.classLoader)!!
        place = parcel.readParcelable(Place::class.java.classLoader)!!
    }

    val task: Long
        get() = geofence.task

    val latitude: Double
        get() = place.latitude

    val longitude: Double
        get() = place.longitude

    val radius: Int
        get() = geofence.radius

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

    fun open(context: Context?) {
        place.open(context)
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(geofence, flags)
        dest.writeParcelable(place, flags)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Location) return false

        if (geofence != other.geofence) return false
        if (place != other.place) return false

        return true
    }

    override fun hashCode(): Int {
        var result = geofence.hashCode()
        result = 31 * result + place.hashCode()
        return result
    }

    override fun toString(): String = "Location(geofence=$geofence, place=$place)"

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<Location> = object : Parcelable.Creator<Location> {
            override fun createFromParcel(`in`: Parcel): Location = Location(`in`)

            override fun newArray(size: Int): Array<Location?> = arrayOfNulls(size)
        }
    }
}