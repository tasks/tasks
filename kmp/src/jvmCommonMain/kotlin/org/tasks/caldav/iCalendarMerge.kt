package org.tasks.caldav

import net.fortuna.ical4j.model.property.Status
import org.tasks.caldav.iCalendar.Companion.collapsed
import org.tasks.caldav.iCalendar.Companion.getLocal
import org.tasks.caldav.iCalendar.Companion.order
import org.tasks.caldav.iCalendar.Companion.parent
import org.tasks.caldav.iCalendar.Companion.toMillis
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task.Priority.Companion.HIGH
import org.tasks.data.entity.Task.Priority.Companion.LOW
import org.tasks.data.entity.Task.Priority.Companion.MEDIUM
import org.tasks.data.entity.Task.Priority.Companion.NONE
import org.tasks.data.setRecurrence
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.time.DateTime.Companion.UTC
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfSecond

fun org.tasks.data.entity.Task.applyRemote(
    remote: VTodoTask,
    local: VTodoTask?
): org.tasks.data.entity.Task {
    applyCompletedAt(remote, local)
    applyCreatedAt(remote, local)
    applyTitle(remote, local)
    applyDescription(remote, local)
    applyPriority(remote, local)
    applyRecurrence(remote, local)
    applyDue(remote, local)
    applyStart(remote, local)
    applyCollapsed(remote, local)
    applyOrder(remote, local)
    return this
}

fun CaldavTask.applyRemote(remote: VTodoTask, local: VTodoTask?): CaldavTask {
    applyParent(remote, local)
    return this
}

private fun org.tasks.data.entity.Task.applyCompletedAt(remote: VTodoTask, local: VTodoTask?) {
    if (local == null ||
        (local.completedAt?.let { getLocal(it) } ?: 0) == completionDate.startOfSecond() &&
        (local.status == Status.VTODO_COMPLETED) == isCompleted
    ) {
        val completedAt = remote.completedAt
        if (completedAt != null) {
            completionDate = getLocal(completedAt)
        } else if (remote.status === Status.VTODO_COMPLETED) {
            if (!isCompleted) {
                completionDate = currentTimeMillis()
            }
        } else {
            completionDate = 0L
        }
    }
}

private fun org.tasks.data.entity.Task.applyCreatedAt(remote: VTodoTask, local: VTodoTask?) {
    val localCreated = local?.createdAt?.let { newDateTime(it, UTC) }?.toLocal()?.millis
    if (localCreated == null || localCreated == creationDate) {
        remote.createdAt?.let {
            creationDate = newDateTime(it, UTC).toLocal().millis
        }
    }
}

private fun org.tasks.data.entity.Task.applyTitle(remote: VTodoTask, local: VTodoTask?) {
    if (local == null || local.summary == title) {
        title = remote.summary
    }
}

private fun org.tasks.data.entity.Task.applyDescription(remote: VTodoTask, local: VTodoTask?) {
    if (local == null || local.description == notes) {
        notes = remote.description
    }
}

private fun org.tasks.data.entity.Task.applyPriority(remote: VTodoTask, local: VTodoTask?) {
    if (local == null || local.tasksPriority == priority) {
        priority = remote.tasksPriority
    }
}

private fun org.tasks.data.entity.Task.applyRecurrence(remote: VTodoTask, local: VTodoTask?) {
    if (local == null || local.rRule?.recur?.toString() == recurrence) {
        setRecurrence(remote.rRule?.recur)
    }
}

private fun org.tasks.data.entity.Task.applyDue(remote: VTodoTask, local: VTodoTask?) {
    if (local == null || local.due.toMillis() == dueDate) {
        dueDate = remote.due.toMillis()
    }
}

private fun org.tasks.data.entity.Task.applyStart(remote: VTodoTask, local: VTodoTask?) {
    if (local == null || local.dtStart.toMillis(this) == hideUntil) {
        hideUntil = remote.dtStart.toMillis(this)
    }
}

private fun org.tasks.data.entity.Task.applyCollapsed(remote: VTodoTask, local: VTodoTask?) {
    if (local == null || isCollapsed == local.collapsed) {
        isCollapsed = remote.collapsed
    }
}

private fun org.tasks.data.entity.Task.applyOrder(remote: VTodoTask, local: VTodoTask?) {
    if (local == null || local.order == order) {
        order = remote.order
    }
}

private fun CaldavTask.applyParent(remote: VTodoTask, local: VTodoTask?) {
    if (local == null || local.parent == remoteParent) {
        remoteParent = remote.parent
    }
}

private val VTodoTask.tasksPriority: Int
    get() = when (this.priority) {
        // https://tools.ietf.org/html/rfc5545#section-3.8.1.9
        in 1..4 -> HIGH
        5 -> MEDIUM
        in 6..9 -> LOW
        else -> NONE
    }