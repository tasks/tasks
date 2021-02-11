package org.tasks.location

import android.os.Parcel
import android.os.Parcelable

class MapPosition : Parcelable {
    val latitude: Double
    val longitude: Double
    val zoom: Float

    @JvmOverloads
    constructor(latitude: Double, longitude: Double, zoom: Float = 15.0f) {
        this.latitude = latitude
        this.longitude = longitude
        this.zoom = zoom
    }

    private constructor(parcel: Parcel) {
        latitude = parcel.readDouble()
        longitude = parcel.readDouble()
        zoom = parcel.readFloat()
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeDouble(latitude)
        dest.writeDouble(longitude)
        dest.writeFloat(zoom)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapPosition) return false

        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (zoom != other.zoom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + zoom.hashCode()
        return result
    }

    override fun toString() = "MapPosition(latitude=$latitude, longitude=$longitude, zoom=$zoom)"

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<MapPosition> = object : Parcelable.Creator<MapPosition> {
            override fun createFromParcel(source: Parcel) = MapPosition(source)

            override fun newArray(size: Int): Array<MapPosition?> = arrayOfNulls(size)
        }
    }
}