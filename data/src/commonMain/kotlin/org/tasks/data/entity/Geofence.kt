package org.tasks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.tasks.CommonParcelable
import org.tasks.CommonParcelize
import org.tasks.data.db.Table

@Serializable
@CommonParcelize
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
) : java.io.Serializable, CommonParcelable {
    companion object {
        const val TABLE_NAME = "geofences"
        @JvmField val TABLE = Table(TABLE_NAME)
        @JvmField val TASK = TABLE.column("task")
        @JvmField val PLACE = TABLE.column("place")
    }
}