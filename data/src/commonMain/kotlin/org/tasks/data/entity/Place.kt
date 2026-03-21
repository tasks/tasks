package org.tasks.data.entity


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.tasks.CommonParcelable
import org.tasks.CommonParcelize
import org.tasks.data.NO_ORDER
import org.tasks.data.Redacted
import org.tasks.data.UUIDHelper
import org.tasks.data.db.Table
import org.tasks.formatCoordinates

@Serializable
@CommonParcelize
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
    @Redacted
    @ColumnInfo(name = "name")
    val name: String? = null,
    @Redacted
    @ColumnInfo(name = "address")
    val address: String? = null,
    @Redacted
    @ColumnInfo(name = "phone")
    val phone: String? = null,
    @Redacted
    @ColumnInfo(name = "url")
    val url: String? = null,
    @Redacted
    @ColumnInfo(name = "latitude")
    val latitude: Double = 0.0,
    @Redacted
    @ColumnInfo(name = "longitude")
    val longitude: Double = 0.0,
    @ColumnInfo(name = "place_color")
    val color: Int = 0,
    @ColumnInfo(name = "place_icon")
    val icon: String? = null,
    @ColumnInfo(name = "place_order")
    val order: Int = NO_ORDER,
    @ColumnInfo(name = "radius", defaultValue = "250")
    val radius: Int = 250,
) : CommonParcelable {
    val displayAddress: String?
        get() = if (address.isNullOrEmpty()) null else address.replace("$name, ", "")

    val displayName: String
        get() {
            if (!name.isNullOrEmpty() && !COORDS.matches(name)) {
                return name
            }
            return if (!address.isNullOrEmpty()) {
                address
            } else {
                "${formatCoordinates(latitude, true)} ${formatCoordinates(longitude, false)}"
            }
        }

    /** Returns distance in meters to another place using the haversine formula. */
    fun distanceTo(other: Place): Double = distanceBetween(
        latitude, longitude,
        other.latitude, other.longitude,
    )

    companion object {
        private val COORDS = Regex("^\\d+°\\d+'\\d+\\.\\d+\"[NS] \\d+°\\d+'\\d+\\.\\d+\"[EW]$")
        const val KEY = "place"
        const val TABLE_NAME = "places"
        @JvmField val TABLE = Table(TABLE_NAME)
        @JvmField val UID = TABLE.column("uid")
        @JvmField val NAME = TABLE.column("name")
        @JvmField val ADDRESS = TABLE.column("address")

        /** Returns distance in meters between two coordinates using the haversine formula. */
        fun distanceBetween(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double,
        ): Double {
            fun Double.toRadians() = this * kotlin.math.PI / 180.0
            val earthRadius = 6_371_000.0 // meters
            val dLat = (lat2 - lat1).toRadians()
            val dLon = (lon2 - lon1).toRadians()
            val a = kotlin.math.sin(dLat / 2).let { it * it } +
                    kotlin.math.cos(lat1.toRadians()) *
                    kotlin.math.cos(lat2.toRadians()) *
                    kotlin.math.sin(dLon / 2).let { it * it }
            return earthRadius * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        }
    }
}
