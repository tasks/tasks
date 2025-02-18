package org.tasks.kmp.org.tasks.taskedit

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.tasks.data.Location
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.entity.TaskAttachment
import org.tasks.filters.CaldavFilter

data class TaskEditViewState(
    val task: Task,
    val displayOrder: ImmutableList<Int>,
    val showBeastModeHint: Boolean,
    val showComments: Boolean,
    val showKeyboard: Boolean,
    val backButtonSavesTask: Boolean,
    val isReadOnly: Boolean,
    val linkify: Boolean,
    val alwaysDisplayFullDate: Boolean,
    val showEditScreenWithoutUnlock: Boolean,
    val list: CaldavFilter,
    val location: Location?,
    val tags: ImmutableSet<TagData>,
    val calendar: String?,
    val attachments: ImmutableSet<TaskAttachment> = persistentSetOf(),
    val alarms: ImmutableSet<Alarm>,
    val newSubtasks: ImmutableList<Task> = persistentListOf(),
    val multilineTitle: Boolean,
) {
    val isNew: Boolean
        get() = task.isNew

    val hasParent: Boolean
        get() = task.parent > 0

    val isCompleted: Boolean
        get() = task.completionDate > 0
}
