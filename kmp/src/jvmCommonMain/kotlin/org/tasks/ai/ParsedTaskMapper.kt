package org.tasks.ai

import org.tasks.data.GoogleTask
import org.tasks.data.createDueDate
import org.tasks.data.createHideUntil
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.repeats.RecurrenceUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Applies a model-produced [ParsedTask] onto an already-built, unsaved [task].
 *
 * Every field is defensive: garbage in one field must not lose the whole task, so anything
 * unparseable is dropped and the value already set by `TaskCreator.create()` is kept.
 */
fun ParsedTask.applyTo(
    task: Task,
    lists: List<CaldavFilter>,
    knownTags: List<TagData>,
) {
    // The system default zone, deliberately: createDueDate/createHideUntil re-derive the
    // calendar day in that zone, so parsing in any other zone would shift the date.
    val zoneId: ZoneId = ZoneId.systemDefault()
    title.trim().takeIf { it.isNotBlank() }?.let { task.title = it }

    notes?.trim()?.takeIf { it.isNotBlank() }?.let { task.notes = it }

    priorityConstant()?.let { task.priority = it }

    due?.let { value ->
        parseLocal(value, zoneId)?.let { (millis, hasTime) ->
            task.dueDate = createDueDate(
                if (hasTime) Task.URGENCY_SPECIFIC_DAY_TIME else Task.URGENCY_SPECIFIC_DAY,
                millis,
            )
        }
    }

    start?.let { value ->
        parseLocal(value, zoneId)?.let { (millis, hasTime) ->
            task.hideUntil = task.createHideUntil(
                if (hasTime) Task.HIDE_UNTIL_SPECIFIC_DAY_TIME else Task.HIDE_UNTIL_SPECIFIC_DAY,
                millis,
            )
        }
    }

    recurrence?.trim()?.takeIf { it.isNotBlank() }?.let { rrule ->
        runCatching { RecurrenceUtils.newRecur(rrule) }
            .onSuccess {
                task.recurrence = rrule
                task.repeatFrom = Task.RepeatFrom.DUE_DATE
            }
    }

    // No match leaves no transitory, so basicQuickAddTask falls back to the default list.
    list?.trim()?.takeIf { it.isNotBlank() }?.let { name ->
        lists
            .filter { !it.isReadOnly }
            .firstOrNull { it.title.equals(name, ignoreCase = true) }
            ?.let { filter ->
                if (filter.isGoogleTasks) {
                    task.putTransitory(GoogleTask.KEY, filter.uuid)
                } else {
                    task.putTransitory(CaldavTask.KEY, filter.uuid)
                }
            }
    }

    // Unmatched names are dropped rather than created: getOrCreateTags would let a hallucinated
    // tag permanently pollute the drawer.
    val resolved = tags
        .mapNotNull { name ->
            knownTags.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }?.name
        }
        .distinct()
    if (resolved.isNotEmpty()) {
        task.putTransitory(Tag.KEY, ArrayList(resolved))
    }
}

private fun ParsedTask.priorityConstant(): Int? = when (priority?.trim()?.lowercase()) {
    "high" -> Task.Priority.HIGH
    "medium" -> Task.Priority.MEDIUM
    "low" -> Task.Priority.LOW
    else -> null // "none", unknown, or absent: keep the configured default priority
}

/** Returns epoch millis paired with whether the value carried a time. */
private fun parseLocal(value: String, zoneId: ZoneId): Pair<Long, Boolean>? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    runCatching {
        val dateTime = LocalDateTime.parse(trimmed)
        return dateTime.atZone(zoneId).toInstant().toEpochMilli() to true
    }
    runCatching {
        val date = LocalDate.parse(trimmed)
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli() to false
    }
    return null
}
