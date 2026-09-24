package org.tasks.caldav

import org.tasks.caldav.iCalendar.Companion.collapsed
import org.tasks.caldav.iCalendar.Companion.matchesDue
import org.tasks.caldav.iCalendar.Companion.matchesStart
import org.tasks.caldav.iCalendar.Companion.order
import org.tasks.caldav.iCalendar.Companion.parent
import org.tasks.caldav.iCalendar.Companion.toDueMillis
import org.tasks.caldav.iCalendar.Companion.toStartMillis
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task.Priority.Companion.HIGH
import org.tasks.data.entity.Task.Priority.Companion.LOW
import org.tasks.data.entity.Task.Priority.Companion.MEDIUM
import org.tasks.data.entity.Task.Priority.Companion.NONE
import org.tasks.data.setRecurrence
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.icalendar.TodoStatus
import org.tasks.icalendar.VTodo
import org.tasks.repeats.Recur
import org.tasks.time.DateTime.Companion.UTC
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfSecond

fun org.tasks.data.entity.Task.applyRemote(
    remote: VTodo,
    local: VTodo?
): org.tasks.data.entity.Task {
    applyCompletedAt(remote, local)
    applyCreatedAt(remote, local)
    applyModified(remote, local)
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

fun CaldavTask.applyRemote(remote: VTodo, local: VTodo?): CaldavTask {
    applyParent(remote, local)
    return this
}

private fun org.tasks.data.entity.Task.applyCompletedAt(remote: VTodo, local: VTodo?) {
    if (local == null ||
        (local.completedAt?.startOfSecond() ?: 0) == completionDate.startOfSecond() &&
        (local.status == TodoStatus.COMPLETED) == isCompleted
    ) {
        val completedAt = remote.completedAt?.startOfSecond()
        if (completedAt != null) {
            completionDate = completedAt
        } else if (remote.status == TodoStatus.COMPLETED) {
            if (!isCompleted) {
                completionDate = currentTimeMillis()
            }
        } else {
            completionDate = 0L
        }
    }
}

private fun org.tasks.data.entity.Task.applyCreatedAt(remote: VTodo, local: VTodo?) {
    val localCreated = local?.createdAt?.toLocalMillis()
    if (localCreated == null || localCreated.startOfSecond() == creationDate.startOfSecond()) {
        val remoteCreated = remote.createdAt?.toLocalMillis()
        when {
            remoteCreated != null -> creationDate = remoteCreated
            local == null -> remote.dtStamp?.let { creationDate = it.toLocalMillisClamped() }
        }
    }
}

private fun org.tasks.data.entity.Task.applyModified(remote: VTodo, local: VTodo?) {
    val localModified = local?.lastModified?.toLocalMillis()
    if (localModified == null || localModified.startOfSecond() == modificationDate.startOfSecond()) {
        val remoteModified = remote.lastModified?.toLocalMillisClamped() ?: 0L
        val remoteCreated = remote.createdAt?.toLocalMillisClamped() ?: 0L
        val remoteDtStamp = remote.dtStamp?.toLocalMillisClamped() ?: 0L
        modificationDate = maxOf(remoteModified, remoteCreated).takeIf { it > 0 }
            ?: remoteDtStamp.takeIf { it > 0 }
            ?: modificationDate.takeIf { it > 0 }
            ?: currentTimeMillis()
    }
}

private fun Long.toLocalMillis(): Long = newDateTime(this, UTC).toLocal().millis

private fun Long.toLocalMillisClamped(): Long = toLocalMillis().coerceAtMost(currentTimeMillis())

private fun org.tasks.data.entity.Task.applyTitle(remote: VTodo, local: VTodo?) {
    if (local == null || local.summary == title) {
        title = remote.summary
    }
}

private fun org.tasks.data.entity.Task.applyDescription(remote: VTodo, local: VTodo?) {
    if (local == null || local.description == notes) {
        notes = remote.description
    }
}

private fun org.tasks.data.entity.Task.applyPriority(remote: VTodo, local: VTodo?) {
    if (local == null || local.tasksPriority == priority) {
        priority = remote.tasksPriority
    }
}

private fun org.tasks.data.entity.Task.applyRecurrence(remote: VTodo, local: VTodo?) {
    if (local == null || local.rRule.matchesStored(recurrence)) {
        setRecurrence(remote.rRule)
    }
}

private fun Recur?.matchesStored(recurrence: String?): Boolean {
    if (this?.toString() == recurrence) return true
    if (this == null || unknownParts.isEmpty()) return false
    return copy(unknownParts = emptyList()).toString() == recurrence
}

private fun org.tasks.data.entity.Task.applyDue(remote: VTodo, local: VTodo?) {
    if (local == null || local.due.matchesDue(this)) {
        dueDate = remote.due.toDueMillis()
    }
}

private fun org.tasks.data.entity.Task.applyStart(remote: VTodo, local: VTodo?) {
    if (local == null || local.dtStart.matchesStart(this)) {
        hideUntil = remote.dtStart.toStartMillis(this)
    }
}

private fun org.tasks.data.entity.Task.applyCollapsed(remote: VTodo, local: VTodo?) {
    if (local == null || isCollapsed == local.collapsed) {
        isCollapsed = remote.collapsed
    }
}

private fun org.tasks.data.entity.Task.applyOrder(remote: VTodo, local: VTodo?) {
    if (local == null || local.order == order) {
        order = remote.order
    }
}

private fun CaldavTask.applyParent(remote: VTodo, local: VTodo?) {
    if (local == null || local.parent == remoteParent) {
        remoteParent = remote.parent
    }
}

private val VTodo.tasksPriority: Int
    get() = when (this.priority) {
        // https://tools.ietf.org/html/rfc5545#section-3.8.1.9
        in 1..4 -> HIGH
        5 -> MEDIUM
        in 6..9 -> LOW
        else -> NONE
    }