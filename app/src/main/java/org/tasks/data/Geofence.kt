package org.tasks.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.todoroo.andlib.data.Table
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.preferences.Preferences
import java.io.Serializable

@Entity(
    tableName = Geofence.TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["_id"],
            childColumns = ["task"],
            onDelete = ForeignKey.CASCADE,
        ),
    ]
)
data class Geofence(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "geofence_id")
    @Transient
    val id: Long = 0,
    @ColumnInfo(name = "task", index = true)
    @Transient
    val task: Long = 0,
    @ColumnInfo(name = "place")
    val place: String? = null,
    @ColumnInfo(name = "arrival")
    val isArrival: Boolean = false,
    @ColumnInfo(name = "departure")
    var isDeparture: Boolean = false,
) : Serializable, Parcelable {
    @Ignore
    constructor(
        task: Long,
        place: String?,
        arrival: Boolean,
        departure: Boolean
    ): this(
        task = task,
        place = place,
        isArrival = arrival,
        isDeparture = departure,
    )

    @Ignore
    constructor(
        place: String?,
        preferences: Preferences,
        defaultReminders: Int = preferences.getIntegerFromString(R.string.p_default_location_reminder_key, 1)
    ): this(
        place = place,
        isArrival = defaultReminders == 1 || defaultReminders == 3,
        isDeparture = defaultReminders == 2 || defaultReminders == 3,
    )

    @Ignore
    constructor(
        place: String?,
        arrival: Boolean,
        departure: Boolean
    ): this(
        place = place,
        isArrival = arrival,
        isDeparture = departure,
    )

    @Ignore
    constructor(o: Geofence): this(
        id = o.id,
        task = o.task,
        place = o.place,
        isArrival = o.isArrival,
        isDeparture = o.isDeparture,
    )

    @Ignore
    constructor(parcel: Parcel): this(
        id = parcel.readLong(),
        task = parcel.readLong(),
        place = parcel.readString(),
        isArrival = parcel.readInt() == 1,
        isDeparture = parcel.readInt() == 1,
    )

    override fun describeContents() = 0

    override fun writeToParcel(out: Parcel, flags: Int) {
        with(out) {
            writeLong(id)
            writeLong(task)
            writeString(place)
            writeInt(if (isArrival) 1 else 0)
            writeInt(if (isDeparture) 1 else 0)
        }
    }

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