package org.tasks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.tasks.data.db.Table

@Entity(
    tableName = "task_dirty",
    indices = [Index(value = ["dirty_version", "synced_version"])],
    foreignKeys = [
        ForeignKey(
            entity = CaldavTask::class,
            parentColumns = ["cd_id"],
            childColumns = ["caldav_task_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class TaskDirtyVersion(
    @PrimaryKey
    @ColumnInfo(name = "caldav_task_id")
    val caldavTaskId: Long,
    @ColumnInfo(name = "dirty_version", defaultValue = "0")
    val dirtyVersion: Long = 0,
    @ColumnInfo(name = "synced_version", defaultValue = "0")
    val syncedVersion: Long = 0,
) {
    companion object {
        val TABLE = Table("task_dirty")
        val CALDAV_TASK_ID = TABLE.column("caldav_task_id")
        val DIRTY_VERSION = TABLE.column("dirty_version")
        val SYNCED_VERSION = TABLE.column("synced_version")
    }
}
