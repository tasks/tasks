package org.tasks.data

import androidx.room.*
import com.todoroo.andlib.data.Table

@Entity(tableName = "google_tasks",
        indices = [
            Index(name = "gt_task", value = ["gt_task"]),
            Index(name = "gt_list_parent", value = ["gt_list_id", "gt_parent"])])
class GoogleTask {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "gt_id")
    @Transient
    var id: Long = 0

    @ColumnInfo(name = "gt_task")
    @Transient
    var task: Long = 0

    @ColumnInfo(name = "gt_remote_id")
    var remoteId: String? = ""

    @ColumnInfo(name = "gt_list_id")
    var listId: String? = ""

    @ColumnInfo(name = "gt_parent")
    @Transient
    var parent: Long = 0

    @ColumnInfo(name = "gt_remote_parent")
    var remoteParent: String? = null
        set(value) {
            field = if (value?.isNotBlank() == true) value else null
        }

    @ColumnInfo(name = "gt_moved")
    @Transient
    var isMoved = false

    @ColumnInfo(name = "gt_order")
    @Transient
    var order: Long = 0

    @ColumnInfo(name = "gt_remote_order")
    var remoteOrder: Long = 0

    @ColumnInfo(name = "gt_last_sync")
    var lastSync: Long = 0

    @ColumnInfo(name = "gt_deleted")
    var deleted: Long = 0

    constructor()

    @Ignore
    constructor(task: Long, listId: String) {
        this.task = task
        this.listId = listId
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoogleTask) return false

        if (id != other.id) return false
        if (task != other.task) return false
        if (remoteId != other.remoteId) return false
        if (listId != other.listId) return false
        if (parent != other.parent) return false
        if (remoteParent != other.remoteParent) return false
        if (isMoved != other.isMoved) return false
        if (order != other.order) return false
        if (remoteOrder != other.remoteOrder) return false
        if (lastSync != other.lastSync) return false
        if (deleted != other.deleted) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + task.hashCode()
        result = 31 * result + remoteId.hashCode()
        result = 31 * result + listId.hashCode()
        result = 31 * result + parent.hashCode()
        result = 31 * result + (remoteParent?.hashCode() ?: 0)
        result = 31 * result + isMoved.hashCode()
        result = 31 * result + order.hashCode()
        result = 31 * result + remoteOrder.hashCode()
        result = 31 * result + lastSync.hashCode()
        result = 31 * result + deleted.hashCode()
        return result
    }

    override fun toString(): String {
        return "GoogleTask(id=$id, task=$task, remoteId='$remoteId', listId='$listId', parent=$parent, remoteParent=$remoteParent, isMoved=$isMoved, order=$order, remoteOrder=$remoteOrder, lastSync=$lastSync, deleted=$deleted)"
    }

    val isNew: Boolean
        get() = id == 0L

    companion object {
        const val KEY = "gtasks"
        @JvmField val TABLE = Table("google_tasks")
        val ID = TABLE.column("gt_id")
        @JvmField val PARENT = TABLE.column("gt_parent")
        @JvmField val TASK = TABLE.column("gt_task")
        @JvmField val DELETED = TABLE.column("gt_deleted")
        @JvmField val LIST = TABLE.column("gt_list_id")
    }
}