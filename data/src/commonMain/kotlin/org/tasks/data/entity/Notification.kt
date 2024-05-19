package org.tasks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.tasks.data.db.Table

@Entity(
    tableName = Notification.TABLE_NAME,
    indices = [
        Index(value = ["task"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["_id"],
            childColumns = ["task"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class Notification(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "uid")
    val uid: Long = 0,
    @ColumnInfo(name = "task")
    val taskId: Long = 0,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = 0,
    @ColumnInfo(name = "type")
    val type: Int = 0,
    @ColumnInfo(name = "location")
    val location: Long? = null,
) {
    companion object {
        const val TABLE_NAME = "notification"
        val TABLE = Table(TABLE_NAME)
        val TASK = TABLE.column("task")
    }
}