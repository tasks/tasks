package org.tasks.data.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.tasks.data.db.Table

@Serializable
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
) : java.io.Serializable, Parcelable {
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