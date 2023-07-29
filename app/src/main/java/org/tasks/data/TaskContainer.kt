package org.tasks.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.todoroo.astrid.data.Task
import org.tasks.data.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.CaldavAccount.Companion.TYPE_MICROSOFT

data class TaskContainer(
    @Embedded val task: Task,
    @Embedded val caldavTask: CaldavTask? = null,
    @Embedded val location: Location? = null,
    val accountType: Int = CaldavAccount.TYPE_LOCAL,
    val parentComplete: Boolean = false,
    @ColumnInfo(name = "tags") val tagsString: String? = null,
    val children: Int = 0,
    val sortGroup: Long? = null,
    val primarySort: Long = 0,
    val secondarySort: Long = 0,
    var indent: Int = 0,
    var targetIndent: Int = 0,
){
    val isGoogleTask: Boolean
        get() = accountType == TYPE_GOOGLE_TASKS

    val isSingleLevelSubtask: Boolean
        get() = when (accountType) {
            TYPE_GOOGLE_TASKS, TYPE_MICROSOFT -> true
            else -> false
        }

    val caldav: String?
        get() = caldavTask?.calendar

    fun isCaldavTask(): Boolean = caldavTask != null

    val notes: String?
        get() = task.notes

    fun hasNotes(): Boolean {
        return task.hasNotes()
    }

    val title: String?
        get() = task.title
    val isHidden: Boolean
        get() = task.isHidden
    val isCompleted: Boolean
        get() = task.isCompleted

    fun hasDueDate(): Boolean {
        return task.hasDueDate()
    }

    fun hasDueTime(): Boolean {
        return task.hasDueTime()
    }

    val isOverdue: Boolean
        get() = task.isOverdue
    val dueDate: Long
        get() = task.dueDate
    val id: Long
        get() = task.id

    val creationDate: Long
        get() = task.creationDate

    val uuid: String
        get() = task.uuid
    var parent: Long
        get() = task.parent
        set(parent) {
            task.parent = parent
        }

    fun hasParent(): Boolean = parent > 0

    fun hasChildren(): Boolean = children > 0

    fun hasLocation(): Boolean = location != null

    val isCollapsed: Boolean
        get() = task.isCollapsed
    val caldavSortOrder: Long
        get() = if (indent == 0) primarySort else secondarySort
    val priority: Int
        get() = task.priority

    val isReadOnly: Boolean
        get() = task.readOnly
}