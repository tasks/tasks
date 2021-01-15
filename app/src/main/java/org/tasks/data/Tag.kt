package org.tasks.data

import androidx.room.*
import com.todoroo.andlib.data.Table
import com.todoroo.astrid.data.Task

@Entity(tableName = "tags", indices = [Index(name = "tag_task", value = ["task"])])
class Tag {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    var id: Long = 0

    @ColumnInfo(name = "task")
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

    companion object {
        const val KEY = "tags-tag" // $NON-NLS-1$
        @JvmField val TABLE = Table("tags")
        @JvmField val TASK = TABLE.column("task")
        @JvmField val TAG_UID = TABLE.column("tag_uid")
        @JvmField val NAME = TABLE.column("name")
    }
}