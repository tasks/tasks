package org.tasks.data

import androidx.room.*
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
class CaldavTask {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "cd_id")
    @Transient
    var id: Long = 0

    @ColumnInfo(name = "cd_task", index = true)
    @Transient
    var task: Long = 0

    @ColumnInfo(name = "cd_calendar")
    var calendar: String? = null

    @ColumnInfo(name = "cd_object")
    var `object`: String? = null
        get() = field ?: "$remoteId.ics"

    @ColumnInfo(name = "cd_remote_id")
    var remoteId: String? = null

    @ColumnInfo(name = "cd_etag")
    var etag: String? = null

    @ColumnInfo(name = "cd_last_sync")
    var lastSync: Long = 0

    @ColumnInfo(name = "cd_deleted")
    var deleted: Long = 0

    @ColumnInfo(name = "cd_remote_parent")
    var remoteParent: String? = null

    @ColumnInfo(name = "gt_moved")
    var isMoved: Boolean = false

    @ColumnInfo(name = "gt_remote_order")
    var remoteOrder: Long = 0

    @Transient
    @Ignore
    @Deprecated("For importing old backup files")
    var order: Long? = null

    constructor()

    @Ignore
    constructor(task: Long, calendar: String?) {
        this.task = task
        this.calendar = calendar
        remoteId = UUIDHelper.newUUID()
        `object` = "$remoteId.ics"
    }

    @Ignore
    constructor(task: Long, calendar: String?, remoteId: String?, `object`: String?) {
        this.task = task
        this.calendar = calendar
        this.remoteId = remoteId
        this.`object` = `object`
    }

    fun isDeleted() = deleted > 0

    override fun toString(): String =
            "CaldavTask(id=$id, task=$task, calendar=$calendar, `object`=$`object`, remoteId=$remoteId, etag=$etag, lastSync=$lastSync, deleted=$deleted, remoteParent=$remoteParent)"

    companion object {
        const val KEY = "caldav"
        @JvmField val TABLE = Table("caldav_tasks")
        val ID = TABLE.column("cd_id")
        @JvmField val TASK = TABLE.column("cd_task")
        @JvmField val DELETED = TABLE.column("cd_deleted")
        @JvmField val CALENDAR = TABLE.column("cd_calendar")
    }
}
