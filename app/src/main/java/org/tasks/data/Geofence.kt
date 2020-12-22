package org.tasks.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import com.todoroo.andlib.data.Table
import org.tasks.R
import org.tasks.preferences.Preferences
import java.io.Serializable

@Entity(tableName = Geofence.TABLE_NAME, indices = [Index(name = "geo_task", value = ["task"])])
class Geofence : Serializable, Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "geofence_id")
    @Transient
    var id: Long = 0

    @ColumnInfo(name = "task")
    @Transient
    var task: Long = 0

    @ColumnInfo(name = "place")
    var place: String? = null

    @ColumnInfo(name = "radius")
    var radius = 0

    @ColumnInfo(name = "arrival")
    var isArrival = false

    @ColumnInfo(name = "departure")
    var isDeparture = false

    constructor()

    @Ignore
    constructor(task: Long, place: String?, arrival: Boolean, departure: Boolean, radius: Int) : this(place, arrival, departure, radius) {
        this.task = task
    }

    @Ignore
    constructor(place: String?, preferences: Preferences) {
        this.place = place
        val defaultReminders = preferences.getIntegerFromString(R.string.p_default_location_reminder_key, 1)
        isArrival = defaultReminders == 1 || defaultReminders == 3
        isDeparture = defaultReminders == 2 || defaultReminders == 3
        radius = preferences.getInt(R.string.p_default_location_radius, 250)
    }

    @Ignore
    constructor(place: String?, arrival: Boolean, departure: Boolean, radius: Int) {
        this.place = place
        isArrival = arrival
        isDeparture = departure
        this.radius = radius
    }

    @Ignore
    constructor(o: Geofence) {
        id = o.id
        task = o.task
        place = o.place
        radius = o.radius
        isArrival = o.isArrival
        isDeparture = o.isDeparture
    }

    @Ignore
    constructor(parcel: Parcel) {
        id = parcel.readLong()
        task = parcel.readLong()
        place = parcel.readString()
        radius = parcel.readInt()
        isArrival = parcel.readInt() == 1
        isDeparture = parcel.readInt() == 1
    }

    override fun describeContents() = 0

    override fun writeToParcel(out: Parcel, flags: Int) {
        with(out) {
            writeLong(id)
            writeLong(task)
            writeString(place)
            writeInt(radius)
            writeInt(if (isArrival) 1 else 0)
            writeInt(if (isDeparture) 1 else 0)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Geofence) return false

        if (id != other.id) return false
        if (task != other.task) return false
        if (place != other.place) return false
        if (radius != other.radius) return false
        if (isArrival != other.isArrival) return false
        if (isDeparture != other.isDeparture) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + task.hashCode()
        result = 31 * result + (place?.hashCode() ?: 0)
        result = 31 * result + radius
        result = 31 * result + isArrival.hashCode()
        result = 31 * result + isDeparture.hashCode()
        return result
    }

    override fun toString(): String =
            "Geofence(id=$id, task=$task, place=$place, radius=$radius, isArrival=$isArrival, isDeparture=$isDeparture)"

    companion object {
        const val TABLE_NAME = "geofences"
        @JvmField val TABLE = Table(TABLE_NAME)
        @JvmField val TASK = TABLE.column("task")
        @JvmField val PLACE = TABLE.column("place")
        @JvmField val CREATOR: Parcelable.Creator<Geofence> = object : Parcelable.Creator<Geofence> {
            override fun createFromParcel(source: Parcel): Geofence = Geofence(source)

            override fun newArray(size: Int): Array<Geofence?> = arrayOfNulls(size)
        }
    }
}