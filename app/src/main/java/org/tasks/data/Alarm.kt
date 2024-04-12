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
import org.tasks.time.DateTimeUtils.printTimestamp
import java.util.concurrent.TimeUnit

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
class Alarm : Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    var id: Long = 0

    @ColumnInfo(name = "task", index = true)
    @Transient
    var task: Long = 0

    @ColumnInfo(name = "time")
    var time: Long = 0

    @ColumnInfo(name = "type", defaultValue = "0")
    var type: Int = 0

    @ColumnInfo(name = "repeat", defaultValue = "0")
    var repeat: Int = 0

    @ColumnInfo(name = "interval", defaultValue = "0")
    var interval: Long = 0

    constructor()

    @Ignore
    constructor(parcel: Parcel) {
        id = parcel.readLong()
        task = parcel.readLong()
        time = parcel.readLong()
        type = parcel.readInt()
        repeat = parcel.readInt()
        interval = parcel.readLong()
    }

    @Ignore
    constructor(task: Long, time: Long, type: Int, repeat: Int = 0, interval: Long = 0) {
        this.task = task
        this.time = time
        this.type = type
        this.repeat = repeat
        this.interval = interval
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeLong(task)
        parcel.writeLong(time)
        parcel.writeInt(type)
        parcel.writeInt(repeat)
        parcel.writeLong(interval)
    }

    override fun describeContents() = 0

    override fun toString(): String {
        val timestamp = if (type == TYPE_DATE_TIME) printTimestamp(time) else time
        return "Alarm(id=$id, task=$task, time=$timestamp, type=$type, repeat=$repeat, interval=$interval)"
    }

    fun same(other: Alarm) =
        type == other.type &&
                time == other.time &&
                repeat == other.repeat &&
                interval == other.interval

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Alarm

        if (id != other.id) return false
        if (task != other.task) return false
        if (time != other.time) return false
        if (type != other.type) return false
        if (repeat != other.repeat) return false
        if (interval != other.interval) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + task.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + type
        result = 31 * result + repeat
        result = 31 * result + interval.hashCode()
        return result
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

        fun whenStarted(task: Long) = Alarm(task, 0, TYPE_REL_START)

        fun whenDue(task: Long) = Alarm(task, 0, TYPE_REL_END)

        fun whenOverdue(task: Long) =
            Alarm(task, TimeUnit.DAYS.toMillis(1), TYPE_REL_END, 6, TimeUnit.DAYS.toMillis(1))

        @JvmField
        val CREATOR = object : Parcelable.Creator<Alarm> {
            override fun createFromParcel(parcel: Parcel) = Alarm(parcel)

            override fun newArray(size: Int): Array<Alarm?> = arrayOfNulls(size)
        }
    }
}