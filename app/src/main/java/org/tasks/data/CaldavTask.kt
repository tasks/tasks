package org.tasks.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.todoroo.andlib.data.Table
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.helper.UUIDHelper

@Entity(
    tableName = "caldav_tasks",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["_id"],
            childColumns = ["cd_task"],
            onDelete = ForeignKey.CASCADE,
        ),
    ]
)
data class CaldavTask(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "cd_id")
    @Transient
    val id: Long = 0,
    @ColumnInfo(name = "cd_task", index = true)
    @Transient
    val task: Long,
    @ColumnInfo(name = "cd_calendar")
    var calendar: String?,
    @ColumnInfo(name = "cd_remote_id")
    var remoteId: String? = UUIDHelper.newUUID(),
    @ColumnInfo(name = "cd_object")
    var `object`: String? = remoteId?.let { "$it.ics" },
    @ColumnInfo(name = "cd_etag")
    var etag: String? = null,
    @ColumnInfo(name = "cd_last_sync")
    var lastSync: Long = 0,
    @ColumnInfo(name = "cd_deleted")
    var deleted: Long = 0,
    @ColumnInfo(name = "cd_remote_parent")
    var remoteParent: String? = null,
    @ColumnInfo(name = "gt_moved")
    var isMoved: Boolean = false,
    @ColumnInfo(name = "gt_remote_order")
    var remoteOrder: Long = 0,
) {
    fun isDeleted() = deleted > 0

    companion object {
        const val KEY = "caldav"
        @JvmField val TABLE = Table("caldav_tasks")
        val ID = TABLE.column("cd_id")
        @JvmField val TASK = TABLE.column("cd_task")
        @JvmField val DELETED = TABLE.column("cd_deleted")
        @JvmField val CALENDAR = TABLE.column("cd_calendar")
    }
}
