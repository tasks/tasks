package org.tasks.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
class Alarm {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    var id: Long = 0

    @ColumnInfo(name = "task")
    @Transient
    var task: Long = 0

    @ColumnInfo(name = "time")
    var time: Long = 0

    constructor()

    @Ignore
    constructor(task: Long, time: Long) {
        this.task = task
        this.time = time
    }

    override fun toString(): String {
        return "Alarm(id=$id, task=$task, time=$time)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Alarm) return false

        if (id != other.id) return false
        if (task != other.task) return false
        if (time != other.time) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + task.hashCode()
        result = 31 * result + time.hashCode()
        return result
    }
}