package org.tasks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.tasks.CommonParcelable
import org.tasks.CommonParcelize
import org.tasks.data.db.Table
import org.tasks.time.printTimestamp
import java.util.concurrent.TimeUnit

@CommonParcelize
@Serializable
@Entity(
    tableName = Alarm.TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["_id"],
            childColumns = ["task"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    val id: Long = 0,
    @ColumnInfo(name = "task", index = true)
    @Transient
    val task: Long = 0,
    @ColumnInfo(name = "time")
    val time: Long = 0,
    @ColumnInfo(name = "type", defaultValue = "0")
    val type: Int = 0,
    @ColumnInfo(name = "repeat", defaultValue = "0")
    val repeat: Int = 0,
    @ColumnInfo(name = "interval", defaultValue = "0")
    val interval: Long = 0,
) : CommonParcelable {
    fun same(other: Alarm) =
        type == other.type &&
                time == other.time &&
                repeat == other.repeat &&
                interval == other.interval

    override fun toString(): String {
        val timestamp = if (type == TYPE_DATE_TIME) printTimestamp(time) else time
        return "Alarm(id=$id, task=$task, time=$timestamp, type=$type, repeat=$repeat, interval=$interval)"
    }

    companion object {
        const val TABLE_NAME = "alarms"
        @JvmField val TABLE = Table(TABLE_NAME)
        @JvmField val TASK = TABLE.column("task")
        @JvmField val TYPE = TABLE.column("type")
        @JvmField val TIME = TABLE.column("time")

        const val TYPE_DATE_TIME = 0
        const val TYPE_REL_START = 1
        const val TYPE_REL_END = 2
        const val TYPE_RANDOM = 3
        const val TYPE_SNOOZE = 4
        const val TYPE_GEO_ENTER = 5
        const val TYPE_GEO_EXIT = 6

        fun whenStarted(task: Long) = Alarm(task = task, type = TYPE_REL_START)

        fun whenDue(task: Long) = Alarm(task = task, type = TYPE_REL_END)

        fun whenOverdue(task: Long) =
            Alarm(
                task = task,
                time = TimeUnit.DAYS.toMillis(1),
                type = TYPE_REL_END,
                repeat = 6,
                interval = TimeUnit.DAYS.toMillis(1)
            )
    }
}