package org.tasks.caldav

import at.bitfire.ical4android.Task
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.data.Task.Priority.Companion.HIGH
import com.todoroo.astrid.data.Task.Priority.Companion.LOW
import com.todoroo.astrid.data.Task.Priority.Companion.MEDIUM
import com.todoroo.astrid.data.Task.Priority.Companion.NONE
import net.fortuna.ical4j.model.property.Status
import org.tasks.caldav.iCalendar.Companion.collapsed
import org.tasks.caldav.iCalendar.Companion.getLocal
import org.tasks.caldav.iCalendar.Companion.order
import org.tasks.caldav.iCalendar.Companion.parent
import org.tasks.caldav.iCalendar.Companion.toMillis
import org.tasks.data.CaldavTask
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.time.DateTime.UTC
import org.tasks.time.DateTimeUtils.startOfSecond

fun com.todoroo.astrid.data.Task.applyRemote(
    remote: Task,
    local: Task?
): com.todoroo.astrid.data.Task {
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

fun CaldavTask.applyRemote(remote: Task, local: Task?): CaldavTask {
    applyParent(remote, local)
    return this
}

private fun com.todoroo.astrid.data.Task.applyCompletedAt(remote: Task, local: Task?) {
    if (local == null ||
        (local.completedAt?.let { getLocal(it) } ?: 0) == completionDate.startOfSecond() &&
        (local.status == Status.VTODO_COMPLETED) == isCompleted
    ) {
        val completedAt = remote.completedAt
        if (completedAt != null) {
            completionDate = getLocal(completedAt)
        } else if (remote.status === Status.VTODO_COMPLETED) {
            if (!isCompleted) {
                completionDate = DateUtilities.now()
            }
        } else {
            completionDate = 0L
        }
    }
}

private fun com.todoroo.astrid.data.Task.applyCreatedAt(remote: Task, local: Task?) {
    val localCreated = local?.createdAt?.let { newDateTime(it, UTC) }?.toLocal()?.millis
    if (localCreated == null || localCreated == creationDate) {
        remote.createdAt?.let {
            creationDate = newDateTime(it, UTC).toLocal().millis.coerceAtMost(now())
        }
    }
}

private fun com.todoroo.astrid.data.Task.applyModified(remote: Task, local: Task?) {
    val localModified = local?.lastModified?.let { newDateTime(it, UTC) }?.toLocal()?.millis
    if (localModified == null || localModified == modificationDate) {
        remote.lastModified?.let {
            modificationDate = newDateTime(it, UTC).toLocal().millis.coerceAtMost(now())
        }
    }
}

private fun com.todoroo.astrid.data.Task.applyTitle(remote: Task, local: Task?) {
    if (local == null || local.summary == title) {
        title = remote.summary
    }
}

private fun com.todoroo.astrid.data.Task.applyDescription(remote: Task, local: Task?) {
    if (local == null || local.description == notes) {
        notes = remote.description
    }
}

private fun com.todoroo.astrid.data.Task.applyPriority(remote: Task, local: Task?) {
    if (local == null || local.tasksPriority == priority) {
        priority = remote.tasksPriority
    }
}

private fun com.todoroo.astrid.data.Task.applyRecurrence(remote: Task, local: Task?) {
    if (local == null || local.rRule?.recur?.toString() == recurrence) {
        setRecurrence(remote.rRule?.recur)
    }
}

private fun com.todoroo.astrid.data.Task.applyDue(remote: Task, local: Task?) {
    if (local == null || local.due.toMillis() == dueDate) {
        dueDate = remote.due.toMillis()
    }
}

private fun com.todoroo.astrid.data.Task.applyStart(remote: Task, local: Task?) {
    if (local == null || local.dtStart.toMillis(this) == hideUntil) {
        hideUntil = remote.dtStart.toMillis(this)
    }
}

private fun com.todoroo.astrid.data.Task.applyCollapsed(remote: Task, local: Task?) {
    if (local == null || isCollapsed == local.collapsed) {
        isCollapsed = remote.collapsed
    }
}

private fun com.todoroo.astrid.data.Task.applyOrder(remote: Task, local: Task?) {
    if (local == null || local.order == order) {
        order = remote.order
    }
}

private fun CaldavTask.applyParent(remote: Task, local: Task?) {
    if (local == null || local.parent == remoteParent) {
        remoteParent = remote.parent
    }
}

private val Task.tasksPriority: Int
    get() = when (this.priority) {
        // https://tools.ietf.org/html/rfc5545#section-3.8.1.9
        in 1..4 -> HIGH
        5 -> MEDIUM
        in 6..9 -> LOW
        else -> NONE
    }