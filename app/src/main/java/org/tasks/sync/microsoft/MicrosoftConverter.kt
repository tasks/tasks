package org.tasks.sync.microsoft

import com.todoroo.astrid.data.Task
import org.tasks.data.CaldavTask
import org.tasks.data.TagData
import org.tasks.time.DateTime
import java.text.SimpleDateFormat
import java.util.*

object MicrosoftConverter {

    private const val TYPE_TEXT = "text"

    fun Task.applyRemote(
        remote: Tasks.Task,
        defaultPriority: Int,
    ) {
        title = remote.title
        notes = remote.body?.content?.takeIf { it.isNotBlank() }
        priority = when {
            remote.importance == Tasks.Task.Importance.high -> Task.Priority.HIGH
            priority != Task.Priority.HIGH -> priority
            defaultPriority != Task.Priority.HIGH -> defaultPriority
            else -> Task.Priority.NONE
        }
        completionDate = remote.completedDateTime
            ?.let {
                val tz = TimeZone.getTimeZone(it.timeZone)
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.ssssss")
                    .apply { timeZone = tz }
                    .parse(it.dateTime)
                    ?.time
                    ?.let { ts -> DateTime(ts, tz).toLocal().millis }
                    ?: System.currentTimeMillis()
            }
            ?: 0L
        // checklist to subtasks
        // due date
        // repeat
        // modification date
        // creation date
        // sync reminders
        // sync files
    }

    fun Task.toRemote(caldavTask: CaldavTask, tags: List<TagData>): Tasks.Task {
        return Tasks.Task(
            id = caldavTask.remoteId,
            title = title,
            body = notes?.let {
                Tasks.Task.Body(
                    content = it,
                    contentType = TYPE_TEXT,
                )
            },
            importance = when (priority) {
                Task.Priority.HIGH -> Tasks.Task.Importance.high
                Task.Priority.MEDIUM -> Tasks.Task.Importance.normal
                else -> Tasks.Task.Importance.low
            },
            status = if (isCompleted) {
                Tasks.Task.Status.completed
            } else {
                Tasks.Task.Status.notStarted
            },
            categories = tags.map { it.name!! }.takeIf { it.isNotEmpty() },
            completedDateTime = if (isCompleted) {
                Tasks.Task.CompletedDateTime(
                    dateTime = DateTime(completionDate).toUTC()
                        .toString("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS"),
                    timeZone = "UTC",
                )
            } else {
                null
            }
        )
    }
}