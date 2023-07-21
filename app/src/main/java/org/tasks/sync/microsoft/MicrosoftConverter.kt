package org.tasks.sync.microsoft

import com.todoroo.astrid.data.Task
import org.tasks.data.CaldavTask
import org.tasks.data.TagData
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils.startOfDay
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.Locale
import java.util.TimeZone

object MicrosoftConverter {

    private const val TYPE_TEXT = "text"

    fun Task.applyRemote(
        remote: Tasks.Task,
        defaultPriority: Int,
    ) {
        title = remote.title
        notes = remote.body?.content?.takeIf { remote.body.contentType == "text" && it.isNotBlank() }
        priority = when {
            remote.importance == Tasks.Task.Importance.high -> Task.Priority.HIGH
            priority != Task.Priority.HIGH -> priority
            defaultPriority != Task.Priority.HIGH -> defaultPriority
            else -> Task.Priority.NONE
        }
        completionDate = remote.completedDateTime.toLong(System.currentTimeMillis())
        dueDate = remote.dueDateTime.toLong(0L)
        creationDate = remote.createdDateTime.parseDateTime()
        modificationDate = remote.lastModifiedDateTime.parseDateTime()
        // checklist to subtasks
        // repeat
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
            dueDateTime = if (hasDueDate()) {
                Tasks.Task.DateTime(
                    dateTime = DateTime(dueDate.startOfDay()).toUTC().toString("yyyy-MM-dd'T'HH:mm:ss.SSS0000"),
                    timeZone = "UTC"
                )
            } else {
                null
            },
            lastModifiedDateTime = DateTime(modificationDate).toUTC().toString("yyyy-MM-dd'T'HH:mm:ss.SSS0000'Z'"),
            createdDateTime = DateTime(creationDate).toUTC().toString("yyyy-MM-dd'T'HH:mm:ss.SSS0000'Z'"),
            completedDateTime = if (isCompleted) {
                Tasks.Task.DateTime(
                    dateTime = DateTime(completionDate).toUTC()
                        .toString("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS"),
                    timeZone = "UTC",
                )
            } else {
                null
            },
//            isReminderOn =
        )
    }

    private fun String?.parseDateTime(): Long =
        this
            ?.let { ZonedDateTime.parse(this).toInstant().toEpochMilli() }
            ?: System.currentTimeMillis()

    private fun Tasks.Task.DateTime?.toLong(default: Long): Long =
        this
            ?.let {
                val tz = TimeZone.getTimeZone(it.timeZone)
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.ssssss", Locale.US)
                    .apply { timeZone = tz }
                    .parse(it.dateTime)
                    ?.time
                    ?.let { ts -> DateTime(ts, tz).toLocal().millis }
                    ?: default
            }
            ?: 0L
}