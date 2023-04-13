package org.tasks.data

import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.todoroo.andlib.data.Table
import com.todoroo.astrid.api.FilterListItem.NO_ORDER
import com.todoroo.astrid.helper.UUIDHelper
import org.tasks.Strings
import org.tasks.extensions.Context.openUri
import org.tasks.location.MapPosition
import java.io.Serializable
import java.util.regex.Pattern
import kotlin.math.abs

@Entity(
    tableName = Place.TABLE_NAME,
    indices = [
        Index(name = "place_uid", value = ["uid"], unique = true)
    ],
)
data class Place(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "place_id")
    @Transient
    val id: Long = 0,
    @ColumnInfo(name = "uid")
    val uid: String? = UUIDHelper.newUUID(),
    @ColumnInfo(name = "name")
    val name: String? = null,
    @ColumnInfo(name = "address")
    val address: String? = null,
    @ColumnInfo(name = "phone")
    val phone: String? = null,
    @ColumnInfo(name = "url")
    val url: String? = null,
    @ColumnInfo(name = "latitude")
    val latitude: Double = 0.0,
    @ColumnInfo(name = "longitude")
    val longitude: Double = 0.0,
    @ColumnInfo(name = "place_color")
    val color: Int = 0,
    @ColumnInfo(name = "place_icon")
    val icon: Int = -1,
    @ColumnInfo(name = "place_order")
    val order: Int = NO_ORDER,
    @ColumnInfo(name = "radius", defaultValue = "250")
    val radius: Int = 250,
) : Serializable, Parcelable {
    @Ignore
    constructor(o: Place): this(
        id = o.id,
        uid = o.uid,
        name = o.name,
        address = o.address,
        phone = o.phone,
        url = o.url,
        latitude = o.latitude,
        longitude = o.longitude,
        color = o.color,
        icon = o.icon,
        order = o.order,
        radius = o.radius,
    )

    @Ignore
    constructor(parcel: Parcel): this(
        id = parcel.readLong(),
        uid = parcel.readString(),
        name = parcel.readString(),
        address = parcel.readString(),
        phone = parcel.readString(),
        url = parcel.readString(),
        latitude = parcel.readDouble(),
        longitude = parcel.readDouble(),
        color = parcel.readInt(),
        icon = parcel.readInt(),
        order = parcel.readInt(),
        radius = parcel.readInt(),
    )

    val displayName: String
        get() {
            if (!Strings.isNullOrEmpty(name) && !COORDS.matcher(name!!).matches()) {
                return name
            }
            return if (!Strings.isNullOrEmpty(address)) {
                address!!
            } else {
                "${formatCoordinate(latitude, true)} ${formatCoordinate(longitude, false)}"
            }
        }

    val displayAddress: String?
        get() = if (Strings.isNullOrEmpty(address)) null else address!!.replace("$name, ", "")

    fun open(context: Context?) =
        context?.openUri("geo:$latitude,$longitude?q=${Uri.encode(displayName)}")

    val mapPosition: MapPosition
        get() = MapPosition(latitude, longitude)

    override fun describeContents() = 0

    override fun writeToParcel(out: Parcel, flags: Int) {
        with(out) {
            writeLong(id)
            writeString(uid)
            writeString(name)
            writeString(address)
            writeString(phone)
            writeString(url)
            writeDouble(latitude)
            writeDouble(longitude)
            writeInt(color)
            writeInt(icon)
            writeInt(order)
            writeInt(radius)
        }
    }

    companion object {
        const val KEY = "place"
        const val TABLE_NAME = "places"
        @JvmField val TABLE = Table(TABLE_NAME)
        @JvmField val UID = TABLE.column("uid")
        @JvmField val NAME = TABLE.column("name")
        @JvmField val ADDRESS = TABLE.column("address")
        @JvmField val CREATOR: Parcelable.Creator<Place> = object : Parcelable.Creator<Place> {
            override fun createFromParcel(source: Parcel): Place = Place(source)

            override fun newArray(size: Int): Array<Place?> = arrayOfNulls(size)
        }
        private val pattern = Pattern.compile("(\\d+):(\\d+):(\\d+\\.\\d+)")
        private val COORDS = Pattern.compile("^\\d+°\\d+'\\d+\\.\\d+\"[NS] \\d+°\\d+'\\d+\\.\\d+\"[EW]$")
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

    }
}