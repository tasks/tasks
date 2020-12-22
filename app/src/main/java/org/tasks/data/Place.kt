package org.tasks.data

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.todoroo.andlib.data.Table
import com.todoroo.astrid.api.FilterListItem.NO_ORDER
import com.todoroo.astrid.helper.UUIDHelper
import net.fortuna.ical4j.model.property.Geo
import org.tasks.Strings
import org.tasks.extensions.safeStartActivity
import org.tasks.location.MapPosition
import org.tasks.themes.CustomIcons.PLACE
import java.io.Serializable
import java.util.regex.Pattern
import kotlin.math.abs

@Entity(tableName = Place.TABLE_NAME, indices = [Index(name = "place_uid", value = ["uid"], unique = true)])
class Place : Serializable, Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "place_id")
    @Transient
    var id: Long = 0

    @ColumnInfo(name = "uid")
    var uid: String? = null

    @ColumnInfo(name = "name")
    var name: String? = null

    @ColumnInfo(name = "address")
    var address: String? = null

    @ColumnInfo(name = "phone")
    var phone: String? = null

    @ColumnInfo(name = "url")
    var url: String? = null

    @ColumnInfo(name = "latitude")
    var latitude = 0.0

    @ColumnInfo(name = "longitude")
    var longitude = 0.0

    @ColumnInfo(name = "place_color")
    var color = 0

    @ColumnInfo(name = "place_icon")
    private var icon = -1

    @ColumnInfo(name = "place_order")
    var order = NO_ORDER

    constructor()

    @Ignore
    constructor(o: Place) {
        id = o.id
        uid = o.uid
        name = o.name
        address = o.address
        phone = o.phone
        url = o.url
        latitude = o.latitude
        longitude = o.longitude
        color = o.color
        icon = o.icon
        order = o.order
    }

    @Ignore
    constructor(parcel: Parcel) {
        id = parcel.readLong()
        uid = parcel.readString()
        name = parcel.readString()
        address = parcel.readString()
        phone = parcel.readString()
        url = parcel.readString()
        latitude = parcel.readDouble()
        longitude = parcel.readDouble()
        color = parcel.readInt()
        icon = parcel.readInt()
        order = parcel.readInt()
    }

    fun getIcon(): Int = if (icon == -1) PLACE else icon

    fun setIcon(icon: Int) {
        this.icon = icon
    }

    val displayName: String
        get() {
            if (!Strings.isNullOrEmpty(name) && !COORDS.matcher(name!!).matches()) {
                return name!!
            }
            return if (!Strings.isNullOrEmpty(address)) {
                address!!
            } else {
                "${formatCoordinate(latitude, true)} ${formatCoordinate(longitude, false)}"
            }
        }

    val displayAddress: String?
        get() = if (Strings.isNullOrEmpty(address)) null else address!!.replace("$name, ", "")

    fun open(context: Context?) {
        context?.safeStartActivity(
                Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("geo:$latitude,$longitude?q=${Uri.encode(displayName)}")
                )
        )
    }

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
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Place) return false

        if (id != other.id) return false
        if (uid != other.uid) return false
        if (name != other.name) return false
        if (address != other.address) return false
        if (phone != other.phone) return false
        if (url != other.url) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (color != other.color) return false
        if (icon != other.icon) return false
        if (order != other.order) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (uid?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (address?.hashCode() ?: 0)
        result = 31 * result + (phone?.hashCode() ?: 0)
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + color
        result = 31 * result + icon
        result = 31 * result + order
        return result
    }

    override fun toString(): String =
            "Place(id=$id, uid=$uid, name=$name, address=$address, phone=$phone, url=$url, latitude=$latitude, longitude=$longitude, color=$color, icon=$icon, order=$order)"

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

        @JvmStatic fun newPlace(geo: Geo): Place = newPlace().apply {
            latitude = geo.latitude.toDouble()
            longitude = geo.longitude.toDouble()
        }

        @JvmStatic fun newPlace(mapPosition: MapPosition?): Place? {
            if (mapPosition == null) {
                return null
            }

            return newPlace().apply {
                latitude = mapPosition.latitude
                longitude = mapPosition.longitude
            }
        }

        @JvmStatic fun newPlace(feature: CarmenFeature): Place = newPlace().apply {
            val types = feature.placeType()

            name = if (types != null && types.contains(GeocodingCriteria.TYPE_ADDRESS)) {
                "${feature.address()} ${feature.text()}"
            } else {
                feature.text()
            }

            address = feature.placeName()
            latitude = feature.center()!!.latitude()
            longitude = feature.center()!!.longitude()
        }

        @JvmStatic fun newPlace(): Place = Place().apply { uid = UUIDHelper.newUUID() }
    }
}