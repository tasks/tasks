package org.tasks.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.todoroo.andlib.data.Table
import com.todoroo.astrid.data.Task

@Entity(
    tableName = "tags",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["_id"],
            childColumns = ["task"],
            onDelete = ForeignKey.CASCADE,
        ),
    ]
)
class Tag {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    var id: Long = 0

    @ColumnInfo(name = "task", index = true)
    @Transient
    var task: Long = 0

    @ColumnInfo(name = "name")
    var name: String? = null

    @ColumnInfo(name = "tag_uid")
    var tagUid: String? = null

    @ColumnInfo(name = "task_uid")
    @Transient
    private var taskUid: String? = null

    constructor()

    @Ignore
    constructor(task: Task, tagData: TagData) : this(task, tagData.name, tagData.remoteId)

    @Ignore
    constructor(task: Task, name: String?, tagUid: String?) : this(task.id, task.uuid, name, tagUid)

    @Ignore
    constructor(taskId: Long, taskUid: String?, name: String?, tagUid: String?) {
        task = taskId
        this.taskUid = taskUid
        this.name = name
        this.tagUid = tagUid
    }

    fun getTaskUid(): String = taskUid!!

    fun setTaskUid(taskUid: String) {
        this.taskUid = taskUid
    }

    override fun toString(): String =
        "Tag(id=$id, task=$task, name=$name, tagUid=$tagUid, taskUid=$taskUid)"

    companion object {
        const val KEY = "tags-tag" // $NON-NLS-1$
        @JvmField val TABLE = Table("tags")
        @JvmField val TASK = TABLE.column("task")
        @JvmField val TAG_UID = TABLE.column("tag_uid")
        @JvmField val NAME = TABLE.column("name")
    }
}