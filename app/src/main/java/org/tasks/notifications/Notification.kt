package org.tasks.notifications

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.todoroo.andlib.data.Table

@Entity(tableName = Notification.TABLE_NAME, indices = [Index(value = ["task"], unique = true)])
class Notification {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "uid")
    var uid = 0

    @ColumnInfo(name = "task")
    var taskId: Long = 0

    @ColumnInfo(name = "timestamp")
    var timestamp: Long = 0

    @ColumnInfo(name = "type")
    var type = 0

    @ColumnInfo(name = "location")
    var location: Long? = null

    override fun toString(): String =
            "Notification(uid=$uid, taskId=$taskId, timestamp=$timestamp, type=$type, location=$location)"

    companion object {
        const val TABLE_NAME = "notification"
        val TABLE = Table(TABLE_NAME)
        val TASK = TABLE.column("task")
    }
}