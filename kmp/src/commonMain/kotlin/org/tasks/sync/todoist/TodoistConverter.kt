package org.tasks.sync.todoist

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.tasks.data.createDueDate
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.repeats.Recur
import org.tasks.time.DateTime

object TodoistConverter {
    private const val DATE_FORMAT = "yyyy-MM-dd"
    private const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"

    fun Task.applyRemote(remote: TodoistItem) {
        title = remote.content.takeIf { it.isNotBlank() }
        notes = remote.description.takeIf { it.isNotBlank() }
        priority = remote.priority.toTaskPriority()
        completionDate = if (remote.checked) {
            remote.completedAt.parseTimestamp() ?: DateTime().millis
        } else {
            0L
        }
        remote.addedAt.parseTimestamp()?.let { creationDate = it }
        remote.updatedAt.parseTimestamp()?.let { modificationDate = it }

        val due = remote.due
        if (due == null) {
            setDueDateAdjustingHideUntil(0L)
        } else {
            due.toDueDate()?.let { (millis, hasTime) ->
                setDueDateAdjustingHideUntil(
                    createDueDate(
                        if (hasTime) Task.URGENCY_SPECIFIC_DAY_TIME else Task.URGENCY_SPECIFIC_DAY,
                        millis,
                    )
                )
            }
        }

        if (due?.isRecurring == true) {
            TodoistRecurrence.fromString(due.string)?.let { parsed ->
                recurrence = parsed.recur.toString()
                repeatFrom = parsed.repeatFrom
            }
        } else {
            recurrence = null
        }
    }

    data class ArgTarget(
        val remoteId: String? = null,
        val projectId: String? = null,
        val sectionId: String? = null,
        val parentId: String? = null,
        val previousDueString: String? = null,
    )

    fun Task.toArgs(
        tags: List<TagData>,
        target: ArgTarget = ArgTarget(),
    ): JsonObject = buildJsonObject {
        target.remoteId?.let { put("id", it) }
        target.projectId?.let { put("project_id", it) }
        target.sectionId?.let { put("section_id", it) }
        target.parentId?.let { put("parent_id", it) }
        put("content", title ?: "")
        put("description", notes ?: "")
        put("priority", priority.toTodoistPriority())
        putJsonArray("labels") {
            tags.mapNotNull { it.name }.forEach { add(it) }
        }
        putDue(this@toArgs, target.previousDueString)
    }

    private fun JsonObjectBuilder.putDue(task: Task, previousDueString: String?) {
        if (!task.hasDueDate()) {
            put("due", kotlinx.serialization.json.JsonNull)
            return
        }
        val dueString = if (task.isRecurring) {
            task.recurrence
                ?.let { runCatching { Recur.parse(it) }.getOrNull() }
                ?.let { TodoistRecurrence.toString(it, task.repeatFrom) }
                ?: previousDueString
        } else {
            null
        }
        val date = DateTime(task.dueDate).let {
            if (task.hasDueTime()) {
                it.withSecondOfMinute(0).withMillisOfSecond(0).toString(DATE_TIME_FORMAT)
            } else {
                it.toString(DATE_FORMAT)
            }
        }
        putJsonObject("due") {
            put("date", date)
            dueString?.let { put("string", it) }
        }
    }

    internal fun Int.toTaskPriority(): Int =
        (TodoistItem.PRIORITY_URGENT - this).coerceIn(Task.Priority.HIGH, Task.Priority.NONE)

    internal fun Int.toTodoistPriority(): Int =
        (TodoistItem.PRIORITY_URGENT - this)
            .coerceIn(TodoistItem.PRIORITY_DEFAULT, TodoistItem.PRIORITY_URGENT)

    internal fun TodoistDue.toDueDate(): Pair<Long, Boolean>? {
        val raw = date.trim().takeIf { it.isNotEmpty() } ?: return null
        if (!raw.contains('T')) {
            val local = runCatching { LocalDate.parse(raw) }.getOrNull() ?: return null
            return DateTime(
                year = local.year,
                month = local.monthNumber,
                day = local.dayOfMonth,
            ).millis to false
        }
        if (raw.endsWith("Z") || OFFSET.containsMatchIn(raw)) {
            runCatching { Instant.parse(raw) }.getOrNull()?.let {
                return it.toEpochMilliseconds() to true
            }
        }
        val local = runCatching { LocalDateTime.parse(raw) }.getOrNull() ?: return null
        return local.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds() to true
    }

    private val OFFSET = Regex("""[+-]\d{2}:?\d{2}$""")

    private fun String?.parseTimestamp(): Long? {
        val raw = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        runCatching { Instant.parse(raw) }.getOrNull()?.let { return it.toEpochMilliseconds() }
        return runCatching {
            LocalDateTime.parse(raw).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        }.getOrNull()
    }
}
