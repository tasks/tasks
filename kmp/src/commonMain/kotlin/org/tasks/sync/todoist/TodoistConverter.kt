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

/**
 * Maps between [Task] and [TodoistItem].
 *
 * Unlike the Microsoft converter this lives in commonMain -- it needs only
 * kotlinx-datetime and [DateTime], both of which are multiplatform.
 */
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

        // Recurrence is natural language on the Todoist side and only partially
        // translatable. When the remote task recurs but we can't parse the rule, keep
        // whatever the task already had rather than clearing a schedule we merely
        // failed to read.
        if (due?.isRecurring == true) {
            TodoistRecurrence.fromString(due.string)?.let { parsed ->
                recurrence = parsed.recur.toString()
                repeatFrom = parsed.repeatFrom
            }
        } else {
            recurrence = null
        }
    }

    /**
     * Builds the `args` object for an `item_add` or `item_update` command.
     *
     * @param remoteId null for a create, otherwise the existing item id.
     * @param projectId target project; ignored by `item_update` (use `item_move`).
     * @param sectionId preserved from the last-synced payload so that editing a task
     *   in Tasks doesn't knock it out of its Todoist section.
     * @param parentId parent item id or temp id, for subtasks.
     * @param previousDueString the last-synced `due.string`, replayed verbatim when
     *   this task's recurrence isn't expressible in Todoist's syntax.
     */
    fun Task.toArgs(
        tags: List<TagData>,
        remoteId: String? = null,
        projectId: String? = null,
        sectionId: String? = null,
        parentId: String? = null,
        previousDueString: String? = null,
    ): JsonObject = buildJsonObject {
        remoteId?.let { put("id", it) }
        projectId?.let { put("project_id", it) }
        sectionId?.let { put("section_id", it) }
        parentId?.let { put("parent_id", it) }
        put("content", title ?: "")
        put("description", notes ?: "")
        put("priority", priority.toTodoistPriority())
        putJsonArray("labels") {
            tags.mapNotNull { it.name }.forEach { add(it) }
        }
        putDue(this@toArgs, previousDueString)
    }

    private fun JsonObjectBuilder.putDue(task: Task, previousDueString: String?) {
        if (!task.hasDueDate()) {
            // An explicit null clears the due date; omitting the key would leave it.
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
                // Tasks encodes "this due date carries a time" as a non-zero seconds
                // field (see createDueDate), so the seconds are a sentinel rather
                // than real data and must not be sent to Todoist.
                it.withSecondOfMinute(0).withMillisOfSecond(0).toString(DATE_TIME_FORMAT)
            } else {
                it.toString(DATE_FORMAT)
            }
        }
        putJsonObject("due") {
            put("date", date)
            // `string` is what Todoist re-parses to derive the repeat schedule, so it
            // must be sent whenever the task recurs -- a bare date would drop it.
            dueString?.let { put("string", it) }
        }
    }

    /** Todoist priority 1..4 (4 = urgent) to [Task.Priority] 0..3 (0 = high). */
    internal fun Int.toTaskPriority(): Int =
        (TodoistItem.PRIORITY_URGENT - this).coerceIn(Task.Priority.HIGH, Task.Priority.NONE)

    internal fun Int.toTodoistPriority(): Int =
        (TodoistItem.PRIORITY_URGENT - this)
            .coerceIn(TodoistItem.PRIORITY_DEFAULT, TodoistItem.PRIORITY_URGENT)

    /**
     * Todoist dates come in three shapes; which one determines whether the result
     * carries a time of day.
     *
     * @return millis and whether a time was present, or null if unparseable.
     */
    internal fun TodoistDue.toDueDate(): Pair<Long, Boolean>? {
        val raw = date.trim().takeIf { it.isNotEmpty() } ?: return null
        if (!raw.contains('T')) {
            // Floating all-day date.
            val local = runCatching { LocalDate.parse(raw) }.getOrNull() ?: return null
            return DateTime(
                year = local.year,
                month = local.monthNumber,
                day = local.dayOfMonth,
            ).millis to false
        }
        // Fixed instant: trailing Z or an explicit offset.
        if (raw.endsWith("Z") || OFFSET.containsMatchIn(raw)) {
            runCatching { Instant.parse(raw) }.getOrNull()?.let {
                return it.toEpochMilliseconds() to true
            }
        }
        // Floating datetime -- interpret in the device's zone, which is what Todoist
        // clients do for a time with no zone attached.
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
