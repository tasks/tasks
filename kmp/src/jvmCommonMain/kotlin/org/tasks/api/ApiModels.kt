package org.tasks.api

import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

data class TaskRow(
    val id: Long,
    val title: String,
    val notes: String?,
    val priority: String,
    val due: Long?,
    val dueAllDay: Boolean,
    val start: Long?,
    val startAllDay: Boolean,
    val completed: Long?,
    val created: Long?,
    val modified: Long?,
    val recurrence: String?,
    val repeatFrom: String?,
    val parentId: Long?,
    val listId: Long?,
    val tagIds: List<Long>,
    val placeId: Long?,
    val childCount: Int,
    val uncompletedChildCount: Int,
    val isReadOnly: Boolean,
)

data class ReminderRow(
    val id: Long,
    val taskId: Long,
    val type: String,
    val placeId: Long?,
    val triggerAt: Long?,
    val offsetMs: Long?,
    val repeatCount: Int?,
    val intervalMs: Long?,
) {
    val isRelative: Boolean get() = type in TasksContract.Alarms.RELATIVE_TYPES
}

data class ListRow(
    val id: Long,
    val title: String,
    val color: Int?,
    val icon: String?,
    val access: String,
    val accountId: Long?,
    val isReadOnly: Boolean,
)

data class TagRow(
    val id: Long,
    val name: String,
    val color: Int?,
    val icon: String?,
)

data class PlaceRow(
    val id: Long,
    val name: String?,
    val displayName: String,
    val address: String?,
    val phone: String?,
    val url: String?,
    val latitude: Double,
    val longitude: Double,
    val radius: Int,
    val color: Int?,
    val icon: String?,
)

data class AccountRow(
    val id: Long,
    val name: String?,
    val type: String,
    val username: String?,
    val url: String?,
    val error: String?,
    val repeatsOnServer: Boolean,
)

fun ApiRow.toTaskRow(): TaskRow {
    val t = TasksContract.Tasks
    return TaskRow(
        id = long(TasksContract.ID),
        title = string(t.TITLE),
        notes = stringOrNull(t.NOTES),
        priority = stringOrNull(t.PRIORITY) ?: TasksContract.Tasks.PRIORITY_NONE,
        due = longOrNull(t.DUE_DATE),
        dueAllDay = boolean(t.DUE_ALL_DAY),
        start = longOrNull(t.START_DATE),
        startAllDay = boolean(t.START_ALL_DAY),
        completed = longOrNull(t.COMPLETED_AT),
        created = longOrNull(t.CREATED_AT),
        modified = longOrNull(t.MODIFIED_AT),
        recurrence = stringOrNull(t.RECURRENCE),
        repeatFrom = stringOrNull(t.REPEAT_FROM),
        parentId = longOrNull(t.PARENT_ID),
        listId = longOrNull(t.LIST_ID),
        tagIds = string(t.TAG_IDS).split(',').mapNotNull { it.toLongOrNull() },
        placeId = longOrNull(t.PLACE_ID),
        childCount = int(t.CHILD_COUNT),
        uncompletedChildCount = int(t.UNCOMPLETED_CHILD_COUNT),
        isReadOnly = boolean(t.IS_READ_ONLY),
    )
}

fun ApiRow.toReminderRow(): ReminderRow {
    val a = TasksContract.Alarms
    val type = string(a.TYPE)
    val offset = longOrNull(a.OFFSET_MS)
    return ReminderRow(
        id = long(TasksContract.ID),
        taskId = long(a.TASK_ID),
        type = type,
        placeId = longOrNull(a.PLACE_ID)?.takeIf { type in TasksContract.Alarms.LOCATION_TYPES },
        triggerAt = longOrNull(a.TRIGGER_AT)?.takeIf { type in TasksContract.Alarms.ABSOLUTE_TYPES },
        offsetMs = offset?.takeIf { type in TasksContract.Alarms.RELATIVE_TYPES },
        repeatCount = int(a.REPEAT_COUNT).takeIf { it != 0 },
        intervalMs = longOrNull(a.INTERVAL_MS),
    )
}

fun ApiRow.toListRow(): ListRow {
    val l = TasksContract.Lists
    val access = stringOrNull(l.ACCESS) ?: TasksContract.Lists.ACCESS_OWNER
    return ListRow(
        id = long(TasksContract.ID),
        title = string(l.TITLE),
        color = int(l.COLOR).takeIf { it != 0 },
        icon = stringOrNull(l.ICON),
        access = access,
        accountId = longOrNull(l.ACCOUNT_ID),
        isReadOnly = access == TasksContract.Lists.ACCESS_READ_ONLY,
    )
}

fun ApiRow.toTagRow(): TagRow {
    val t = TasksContract.Tags
    return TagRow(
        id = long(TasksContract.ID),
        name = string(t.NAME),
        color = int(t.COLOR).takeIf { it != 0 },
        icon = stringOrNull(t.ICON),
    )
}

fun ApiRow.toPlaceRow(): PlaceRow {
    val p = TasksContract.Places
    return PlaceRow(
        id = long(TasksContract.ID),
        name = stringOrNull(p.NAME),
        displayName = string(p.DISPLAY_NAME),
        address = stringOrNull(p.ADDRESS),
        phone = stringOrNull(p.PHONE),
        url = stringOrNull(p.URL),
        latitude = double(p.LATITUDE),
        longitude = double(p.LONGITUDE),
        radius = int(p.RADIUS),
        color = int(p.COLOR).takeIf { it != 0 },
        icon = stringOrNull(p.ICON),
    )
}

fun ApiRow.toAccountRow(): AccountRow {
    val a = TasksContract.Accounts
    return AccountRow(
        id = long(TasksContract.ID),
        name = stringOrNull(a.NAME),
        type = string(a.TYPE),
        username = stringOrNull(a.USERNAME),
        url = stringOrNull(a.URL),
        error = stringOrNull(a.ERROR),
        repeatsOnServer = boolean(a.REPEATS_ON_SERVER),
    )
}

fun describeOffset(offsetMs: Long, type: String): String {
    val anchor = when (type) {
        TasksContract.Alarms.TYPE_RELATIVE_START -> "start"
        TasksContract.Alarms.TYPE_RELATIVE_DUE -> "due"
        else -> "due"
    }
    if (offsetMs == 0L) return "at $anchor time"
    val abs = kotlin.math.abs(offsetMs)
    val direction = if (offsetMs < 0) "before" else "after"
    val amount = when {
        abs % TimeUnit.DAYS.toMillis(1) == 0L -> {
            val d = TimeUnit.MILLISECONDS.toDays(abs)
            "$d day${if (d == 1L) "" else "s"}"
        }
        abs % TimeUnit.HOURS.toMillis(1) == 0L -> {
            val h = TimeUnit.MILLISECONDS.toHours(abs)
            "$h hour${if (h == 1L) "" else "s"}"
        }
        else -> {
            val m = TimeUnit.MILLISECONDS.toMinutes(abs)
            "$m minute${if (m == 1L) "" else "s"}"
        }
    }
    return "$amount $direction $anchor"
}

enum class TaskText {
    Title,
    Notes,
    ;

    companion object {
        val BOTH: Set<TaskText> = entries.toSet()

        val NAMES: List<String> = entries.map { it.name.lowercase() }

        fun of(name: String): TaskText = entries.firstOrNull { it.name.equals(name, true) }
            ?: throw IllegalArgumentException(
                "Unknown match field '$name'. Supported: ${NAMES.joinToString(", ")}"
            )
    }
}

fun Regex.matchesAny(task: TaskRow, fields: Set<TaskText>): Boolean = fields.any {
    when (it) {
        TaskText.Title -> containsMatchIn(task.title)
        TaskText.Notes -> task.notes?.let { notes -> containsMatchIn(notes) } == true
    }
}

class ScanResult(val rows: List<TaskRow>, val total: Int)

const val SCAN_CHUNK = 500

suspend fun ApiQueryEngine.scanTasks(
    matches: Regex,
    matchFields: Set<TaskText>,
    limit: Int,
    offset: Int,
    args: (pageLimit: Int, pageOffset: Int) -> ApiQueryArgs,
): ScanResult {
    var seen = 0
    var matched = 0
    val page = ArrayList<TaskRow>()
    while (true) {
        val rows = query(TasksContract.Tasks.PATH, args(SCAN_CHUNK, seen))
        if (rows.isEmpty) break
        for (row in rows) {
            val task = row.toTaskRow()
            if (!matches.matchesAny(task, matchFields)) continue
            if (matched >= offset && page.size < limit) page += task
            matched++
        }
        seen += rows.size
        if (seen >= rows.total) break
    }
    return ScanResult(page, matched)
}

data class TaskQuery(
    val ids: List<Long> = emptyList(),
    val listIds: List<Long> = emptyList(),
    val tagIds: List<Long> = emptyList(),
    val placeIds: List<Long> = emptyList(),
    val priorities: List<String> = emptyList(),
    val parentIds: List<Long> = emptyList(),
    val search: String? = null,
    val status: String? = null,
    val due: String? = null,
    val matches: String? = null,
    val matchCase: Boolean = false,
    val matchFields: List<String> = emptyList(),
    val dueBefore: Long? = null,
    val dueAfter: Long? = null,
    val startBefore: Long? = null,
    val startAfter: Long? = null,
    val completedBefore: Long? = null,
    val completedAfter: Long? = null,
    val createdBefore: Long? = null,
    val createdAfter: Long? = null,
    val modifiedBefore: Long? = null,
    val modifiedAfter: Long? = null,
    val sort: String? = null,
    val sortDesc: Boolean = false,
    val limit: Int? = null,
    val offset: Int? = null,
) {
    val completed: Boolean? =
        taskStatus(status.orNullIfBlank(), completedBefore != null || completedAfter != null)

    val pattern: Regex? = taskPattern(matches, matchCase)

    val fields: Set<TaskText> = taskTextFields(matchFields)

    val take: Int = pageLimit(limit)

    val skip: Int = pageOffset(offset)

    private val window: Pair<Long?, Long?>? = dueWindow(due.orNullIfBlank())

    fun args(chunk: Int, from: Int): ApiQueryArgs {
        val t = TasksContract.Tasks
        val until = dueBefore ?: window?.second
        val since = dueAfter ?: window?.first
        return ApiQueryArgs.build(TasksContract.paramsFor(t.PATH)) {
            putEach(t.PARAM_ID, ids)
            putEach(t.PARAM_LIST, listIds)
            putEach(t.PARAM_TAG, tagIds)
            putEach(t.PARAM_PLACE, placeIds)
            putEach(t.PARAM_PRIORITY, priorities)
            putEach(t.PARAM_PARENT, parentIds)
            putIfNotNull(t.PARAM_SEARCH, search)
            putIfNotNull(t.PARAM_COMPLETED, completed?.let { if (it) "1" else "0" })
            putIfNotNull(t.PARAM_DUE_BEFORE, until)
            putIfNotNull(t.PARAM_DUE_AFTER, since ?: scheduledOnly(until))
            putIfNotNull(t.PARAM_START_BEFORE, startBefore)
            putIfNotNull(t.PARAM_START_AFTER, startAfter ?: scheduledOnly(startBefore))
            putIfNotNull(t.PARAM_COMPLETED_BEFORE, completedBefore)
            putIfNotNull(t.PARAM_COMPLETED_AFTER, completedAfter)
            putIfNotNull(t.PARAM_CREATED_BEFORE, createdBefore)
            putIfNotNull(t.PARAM_CREATED_AFTER, createdAfter)
            putIfNotNull(t.PARAM_MODIFIED_BEFORE, modifiedBefore)
            putIfNotNull(t.PARAM_MODIFIED_AFTER, modifiedAfter)
            putIfNotNull(t.PARAM_SORT, sort.orNullIfBlank())
            if (sortDesc) put(t.PARAM_SORT_DESC, "1")
            put(TasksContract.PARAM_LIMIT, chunk.toString())
            put(TasksContract.PARAM_OFFSET, from.toString())
        }
    }
}

fun dueWindow(name: String?): Pair<Long?, Long?>? {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    fun startOf(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()
    return when (name) {
        null -> null
        "today" -> (startOf(today) - 1) to startOf(today.plusDays(1))
        "tomorrow" -> (startOf(today.plusDays(1)) - 1) to startOf(today.plusDays(2))
        "overdue" -> 0L to System.currentTimeMillis()
        "this_week" -> (startOf(today) - 1) to startOf(today.plusDays(7))
        "has_due_date" -> 0L to null
        "no_due_date" -> null to UNSCHEDULED_BOUND
        else -> throw IllegalArgumentException(
            "Unknown due filter '$name'. Supported: ${DUE_FILTERS.joinToString("|")}"
        )
    }
}

data class TagEdit(
    val taskId: Long,
    val added: Int,
    val removed: Int,
)

suspend fun ApiWriter.editTaskTags(
    taskId: Long,
    current: Set<Long>,
    add: List<Long>,
    remove: List<Long>,
): TagEdit {
    val toAdd = add.filterNot { it in current }
    toAdd.forEach {
        insertTaskTag(
            ApiValues.of(
                TasksContract.TaskTags.TASK_ID to taskId,
                TasksContract.TaskTags.TAG_ID to it,
            )
        )
    }
    return TagEdit(
        taskId = taskId,
        added = toAdd.size,
        removed = remove.distinct().sumOf { deleteTaskTag(taskId, it) },
    )
}

suspend fun ApiQueryEngine.taskRow(id: Long): TaskRow? =
    queryById(TasksContract.Tasks.PATH, id).firstOrNull()?.toTaskRow()

data class Completion(
    val taskIds: List<Long>,
    val rowsChanged: List<Int>,
    val advancedTaskIds: List<Long>,
)

fun advancedSeries(
    recurrenceBefore: Map<Long, String?>,
    completedAfter: Map<Long, Long?>,
): List<Long> = recurrenceBefore
    .filter { (id, recurrence) -> recurrence != null && completedAfter[id] == null }
    .keys
    .toList()

fun taskStatus(name: String?, completionBounded: Boolean = false): Boolean? = when (name) {
    null -> if (completionBounded) true else false
    "open" -> false
    "completed" -> true
    "any" -> null
    else -> throw IllegalArgumentException(
        "Unknown status '$name'. Supported: ${TASK_STATUSES.joinToString("|")}"
    )
}

const val MAX_BATCH = 50

fun requireBatch(size: Int, noun: String = "tasks") {
    require(size > 0) { "A batch of $noun needs at least one entry." }
    require(size <= MAX_BATCH) {
        "A batch takes at most $MAX_BATCH $noun, was $size. Send the rest in another call."
    }
}

fun requireDistinct(ids: List<Long>) {
    ids.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }?.let { (id, count) ->
        throw IllegalArgumentException(
            "Task $id appears $count times. A task may be updated once per batch - merge those " +
                "changes into one entry."
        )
    }
}

val TASK_STATUSES = listOf("open", "completed", "any")

const val UNSCHEDULED_BOUND = 1L

val DUE_FILTERS = listOf("today", "tomorrow", "overdue", "this_week", "has_due_date", "no_due_date")

private fun scheduledOnly(before: Long?): Long? =
    if (before != null && before != UNSCHEDULED_BOUND) 0L else null

suspend fun ApiQueryEngine.findTasks(query: TaskQuery): ApiPage<TaskRow> {
    val pattern = query.pattern
        ?: return query.args(query.take, query.skip)
            .let { this.query(TasksContract.Tasks.PATH, it) }
            .let { ApiPage(it.map { row -> row.toTaskRow() }, it.total, query.skip) }
    val scan = scanTasks(pattern, query.fields, query.take, query.skip, query::args)
    return ApiPage(scan.rows, scan.total, query.skip)
}

suspend fun ApiQueryEngine.countTasks(query: TaskQuery): Int =
    findTasks(query.copy(limit = 0)).total

fun String?.orNullIfBlank(): String? = this?.takeIf { it.isNotBlank() }

fun taskPattern(pattern: String?, caseSensitive: Boolean): Regex? {
    val text = pattern.orNullIfBlank() ?: return null
    val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
    return try {
        Regex(text, options)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("'matches' is not a valid regular expression: ${e.message}")
    }
}

fun taskTextFields(names: List<String>): Set<TaskText> =
    names.map { TaskText.of(it) }.toSet().ifEmpty { TaskText.BOTH }

fun movedToParentList(parentId: Long?, listId: Long?, landedOn: Long?): Boolean =
    parentId != null && parentId != 0L && listId != null && landedOn != listId

data class ListWrite(
    val title: String? = null,
    val accountId: Long? = null,
    val color: Int? = null,
    val icon: String? = null,
) {
    fun toValues(): ApiValues {
        val l = TasksContract.Lists
        return ApiValues.ofNotNull(
            l.TITLE to title,
            l.ACCOUNT_ID to accountId,
            l.COLOR to color,
            l.ICON to icon,
        )
    }
}

data class TagWrite(
    val name: String? = null,
    val color: Int? = null,
    val icon: String? = null,
) {
    fun toValues(): ApiValues {
        val t = TasksContract.Tags
        return ApiValues.ofNotNull(t.NAME to name, t.COLOR to color, t.ICON to icon)
    }
}

data class PlaceWrite(
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val phone: String? = null,
    val url: String? = null,
    val radius: Int? = null,
    val color: Int? = null,
    val icon: String? = null,
) {
    fun toValues(): ApiValues {
        val p = TasksContract.Places
        return ApiValues.ofNotNull(
            p.NAME to name,
            p.LATITUDE to latitude,
            p.LONGITUDE to longitude,
            p.ADDRESS to address,
            p.PHONE to phone,
            p.URL to url,
            p.RADIUS to radius,
            p.COLOR to color,
            p.ICON to icon,
        )
    }
}

data class ReminderWrite(
    val taskId: Long,
    val type: String,
    val triggerAt: Long? = null,
    val offsetMs: Long? = null,
    val repeatCount: Int? = null,
    val intervalMs: Long? = null,
    val placeId: Long? = null,
) {
    fun toValues(): ApiValues {
        val a = TasksContract.Alarms
        return ApiValues.ofNotNull(
            a.TASK_ID to taskId,
            a.TYPE to type,
            a.TRIGGER_AT to triggerAt,
            a.OFFSET_MS to offsetMs,
            a.REPEAT_COUNT to repeatCount,
            a.INTERVAL_MS to intervalMs,
            a.PLACE_ID to placeId,
        )
    }
}

data class TaskWrite(
    val title: String? = null,
    val notes: String? = null,
    val priority: String? = null,
    val due: Long? = null,
    val dueAllDay: Boolean? = null,
    val start: Long? = null,
    val startAllDay: Boolean? = null,
    val recurrence: String? = null,
    val repeatFrom: String? = null,
    val parentId: Long? = null,
    val listId: Long? = null,
    val placeId: Long? = null,
) {
    val isEmpty: Boolean
        get() = title == null && notes == null && priority == null && due == null &&
                dueAllDay == null && start == null && startAllDay == null &&
                recurrence == null && repeatFrom == null && parentId == null &&
                listId == null && placeId == null

    fun toValues(): ApiValues {
        val t = TasksContract.Tasks
        return ApiValues.ofNotNull(
            t.TITLE to title,
            t.NOTES to notes,
            t.PRIORITY to priority.orNullIfBlank(),
            t.DUE_DATE to due,
            t.DUE_ALL_DAY to dueAllDay?.let { if (it) 1 else 0 },
            t.START_DATE to start,
            t.START_ALL_DAY to startAllDay?.let { if (it) 1 else 0 },
            t.RECURRENCE to recurrence,
            t.REPEAT_FROM to repeatFrom.orNullIfBlank(),
            t.PARENT_ID to parentId,
            t.LIST_ID to listId,
            t.PLACE_ID to placeId,
        )
    }
}
