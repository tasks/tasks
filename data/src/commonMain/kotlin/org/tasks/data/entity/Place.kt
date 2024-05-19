package org.tasks.data.entity

import android.location.Location
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.tasks.data.NO_ORDER
import org.tasks.data.UUIDHelper
import org.tasks.data.db.Table
import java.util.regex.Pattern
import kotlin.math.abs

@Serializable
@Parcelize
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
) : java.io.Serializable, Parcelable {
    val displayName: String
        get() {
            if (!name.isNullOrEmpty() && !COORDS.matcher(name!!).matches()) {
                return name
            }
            return if (!address.isNullOrEmpty()) {
                address!!
            } else {
                "${formatCoordinate(latitude, true)} ${formatCoordinate(longitude, false)}"
            }
        }

    val displayAddress: String?
        get() = if (address.isNullOrEmpty()) null else address!!.replace("$name, ", "")

    companion object {
        const val KEY = "place"
        const val TABLE_NAME = "places"
        @JvmField val TABLE = Table(TABLE_NAME)
        @JvmField val UID = TABLE.column("uid")
        @JvmField val NAME = TABLE.column("name")
        @JvmField val ADDRESS = TABLE.column("address")
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