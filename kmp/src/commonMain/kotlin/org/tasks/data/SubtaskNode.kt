package org.tasks.data

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import org.tasks.compose.pickers.NO_DAY
import org.tasks.compose.pickers.NO_TIME
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter

fun subtaskKey(task: Task): String = subtaskKey(task.id, task.remoteId)

fun subtaskKey(id: Long, remoteId: String?): String =
    remoteId?.takeIf { it.isNotBlank() && it != Task.NO_UUID } ?: "id:$id"

data class PendingTask(
    val list: CaldavFilter?,
    val tags: List<TagData> = emptyList(),
    val alarms: ImmutableSet<Alarm> = persistentSetOf(),
    val startDay: Long = NO_DAY,
    val startTime: Int = NO_TIME,
)

data class SubtaskNode(
    val key: String,
    val parentKey: String,
    val sequence: Long,
    val task: Task,
    val stagedCompleted: Boolean? = null,
    val stagedTitle: String? = null,
    val deleted: Boolean = false,
    val moved: Boolean = false,
    val pending: PendingTask? = null,
) {
    val isNew: Boolean get() = pending != null && task.id <= 0
    val pendingUnwritten: Boolean get() = pending != null && task.id > 0
    val id: Long get() = task.id
    val title: String? get() = stagedTitle ?: task.title
    val titleEdited: Boolean get() = stagedTitle != null
    val completed: Boolean get() = stagedCompleted ?: task.isCompleted
    val completionEdited: Boolean get() = stagedCompleted != null
    val needsWriting: Boolean
        get() = isNew || pendingUnwritten || titleEdited || completionEdited || deleted || moved

    fun sameStagingAs(other: SubtaskNode): Boolean =
        parentKey == other.parentKey &&
                sequence == other.sequence &&
                stagedTitle == other.stagedTitle &&
                stagedCompleted == other.stagedCompleted &&
                deleted == other.deleted &&
                moved == other.moved &&
                pending == other.pending &&
                isNew == other.isNew
}

data class SubtaskRow(
    val node: SubtaskNode,
    val indent: Int,
    val children: Int = 0,
    val completed: Boolean = node.completed,
    val remaining: Int = 0,
) {
    val key: String get() = node.key
    val chipCount: Int get() = if (completed) children else remaining
    val collapsed: Boolean get() = node.task.isCollapsed
}

