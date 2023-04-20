package org.tasks.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.todoroo.andlib.data.Table
import com.todoroo.astrid.data.Task
import kotlinx.parcelize.Parcelize
import org.tasks.R
import org.tasks.preferences.Preferences
import java.io.Serializable

@Parcelize
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

    companion object {
        const val TABLE_NAME = "geofences"
        @JvmField val TABLE = Table(TABLE_NAME)
        @JvmField val TASK = TABLE.column("task")
        @JvmField val PLACE = TABLE.column("place")
    }
}