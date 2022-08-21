package org.tasks.location

import android.os.Parcel
import android.os.Parcelable

data class MapPosition(
    val latitude: Double,
    val longitude: Double,
    val zoom: Float = 15.0f,
) : Parcelable {
    private constructor(parcel: Parcel) : this(
        latitude = parcel.readDouble(),
        longitude = parcel.readDouble(),
        zoom = parcel.readFloat(),
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeDouble(latitude)
        dest.writeDouble(longitude)
        dest.writeFloat(zoom)
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<MapPosition> = object : Parcelable.Creator<MapPosition> {
            override fun createFromParcel(source: Parcel) = MapPosition(source)

            override fun newArray(size: Int): Array<MapPosition?> = arrayOfNulls(size)
        }
    }
}