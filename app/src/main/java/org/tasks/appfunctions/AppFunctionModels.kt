package org.tasks.appfunctions

import androidx.appfunctions.AppFunctionSerializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.tasks.api.AccountRow
import org.tasks.api.Completion
import org.tasks.api.Created
import org.tasks.api.Creation
import org.tasks.api.ReminderEdit
import org.tasks.api.ReminderWrite
import org.tasks.api.Revision
import org.tasks.api.ReminderRow
import org.tasks.api.ApiRow
import org.tasks.api.Deletion
import org.tasks.api.ListRow
import org.tasks.api.PlaceRow
import org.tasks.api.TagChange
import org.tasks.api.TagRow
import org.tasks.api.TaskRow
import org.tasks.api.TaskWrite
import org.tasks.api.describeOffset
import org.tasks.api.toAccountRow
import org.tasks.api.toReminderRow
import org.tasks.api.toListRow
import org.tasks.api.toPlaceRow
import org.tasks.api.toTagRow
import org.tasks.api.toTaskRow

/** A task. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppTask(
    /** Identifier of this task on this device. Not stable across installs. */
    val id: Long,
    /** What the task is called. */
    val title: String,
    /** Free-form detail, or null when the task has none. */
    val notes: String? = null,
    /** One of "high", "medium", "low" or "none". */
    val priority: String,
    /** When the task is due, or null when it is unscheduled. */
    val dueDateTime: LocalDateTime? = null,
    /** True when the due date has no meaningful time of day. */
    val dueAllDay: Boolean,
    /** When work on the task can start, or null when unset. */
    val startDateTime: LocalDateTime? = null,
    /** True when the start date has no meaningful time of day. */
    val startAllDay: Boolean,
    /** When the task was completed, or null while it is still open. */
    val completedAt: LocalDateTime? = null,
    /** When the task was created. */
    val createdAt: LocalDateTime? = null,
    /** When the task last changed. */
    val modifiedAt: LocalDateTime? = null,
    /** Recurrence rule in RFC 5545 RRULE form, or null when the task does not repeat. */
    val recurrence: String? = null,
    /**
     * What the next occurrence of a repeating task is measured from: "due_date" or
     * "completion_date". Only meaningful when recurrence is set.
     */
    val repeatFrom: String? = null,
    /** Identifier of the parent task, or null when this is a top-level task. */
    val parentId: Long? = null,
    /** Identifier of the list this task belongs to. A task is always on exactly one list. */
    val listId: Long? = null,
    /** Identifier of the place this task is filed at, or null. */
    val placeId: Long? = null,
    /** Identifiers of the tags this task carries. */
    val tagIds: LongArray,
    /** How many subtasks this task has. */
    val childCount: Int,
    /** How many of this task's subtasks are still open. */
    val uncompletedChildCount: Int,
    /** True when the task is on a list the user cannot write to. */
    val isReadOnly: Boolean,
)

/** The result of completing or reopening tasks. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppCompletion(
    /** The tasks as they are afterwards. */
    val tasks: List<AppTask>,
    /**
     * Identifiers of the repeating tasks that advanced to their next occurrence instead of
     * staying completed. These come back open, with a later date, and that is the write
     * succeeding. Do not send them again - completing them a second time advances the series
     * once more.
     */
    val advancedTaskIds: LongArray,
    /**
     * Identifiers that changed nothing, because those tasks no longer exist. Every other
     * identifier did apply - do not send them again.
     */
    val unchangedTaskIds: LongArray,
    /** Identifiers of subtasks that were completed along with their parent. */
    val alsoCompletedTaskIds: LongArray,
    /**
     * Identifiers of related tasks that were reopened as well: reopening a task reopens its
     * subtasks, and a task with an open subtask is not complete.
     */
    val reopenedTaskIds: LongArray,
)

/** The result of creating tasks. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppTaskCreation(
    /** The created tasks, including the identifiers assigned to them. */
    val tasks: List<AppTask>,
    /**
     * Identifiers of the tasks that were created on their parent's list rather than the listId
     * sent with them. A subtask lives on its parent's list, so parentId decides. Send parentId on
     * its own, or parentId 0 with a listId for a top-level task.
     */
    val movedToParentListTaskIds: LongArray,
)

/** The result of changing tasks. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppTaskUpdate(
    /** The tasks as they are after the change. Entries that changed nothing are left out. */
    val tasks: List<AppTask>,
    /**
     * Identifiers of the entries that changed nothing, because those tasks no longer exist. Every
     * other entry did apply - do not send them again.
     */
    val unchangedTaskIds: LongArray,
    /**
     * Identifiers of the tasks that moved to their new parent's list rather than the listId sent
     * with them. A subtask lives on its parent's list, so parentId decides.
     */
    val movedToParentListTaskIds: LongArray,
    /**
     * Identifiers of the subtasks that became top-level tasks because they were moved to another
     * list on their own. A subtask lives on its parent's list, so send parentId as well to put it
     * under a task there.
     */
    val detachedTaskIds: LongArray,
)

/** The result of deleting something, and what went with it. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppDeletion(
    /** True when the row existed and was deleted, false when nothing matched the identifier. */
    val deleted: Boolean,
    /** How many other rows went with it. What those are depends on what was deleted. */
    val alsoAffected: Int,
)

/** A page of tasks. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppTaskPage(
    /** The tasks in this page. */
    val tasks: List<AppTask>,
    /** How many tasks match in total, ignoring limit and offset. */
    val total: Int,
    /** The offset this page starts at, or null for a count: a limit of zero asks how many. */
    val offset: Int?,
    /** True when more tasks match than were returned. Ask again with a larger offset. */
    val hasMore: Boolean?,
)

/** A task to create. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class NewTask(
    /** What the task is called. Cannot be empty. */
    val title: String,
    /** Free-form notes. Markdown, as the user would type it. */
    val notes: String? = null,
    /** One of "high", "medium", "low" or "none". Defaults to "none". */
    val priority: String? = null,
    /** When the task is due. Leave null for an unscheduled task. */
    val dueDateTime: LocalDateTime? = null,
    /** True when the due date has no meaningful time of day. */
    val dueAllDay: Boolean? = null,
    /** When work on the task can start; the app hides it until then. */
    val startDateTime: LocalDateTime? = null,
    /** True when the start date has no meaningful time of day. */
    val startAllDay: Boolean? = null,
    /** Recurrence rule in RFC 5545 RRULE form, for example "RRULE:FREQ=WEEKLY;BYDAY=MO". */
    val recurrence: String? = null,
    /** What the next occurrence is measured from: "due_date" or "completion_date". */
    val repeatFrom: String? = null,
    /** Identifier of the list to put the task on. Defaults to the user's default list. */
    val listId: Long? = null,
    /**
     * Identifier of the parent task, to create this as a subtask. The parent must already exist -
     * it cannot be a task created earlier in this same call, and an identifier that does not exist
     * refuses the whole batch. To build a task and its subtasks, create the parent, then send the
     * children in a second call with its identifier.
     */
    val parentId: Long? = null,
    /** Identifier of a place to file the task at. No reminder is implied. */
    val placeId: Long? = null,
)

/**
 * A change to one existing task. Every field except the identifier is optional; omit a field to
 * leave it as it is.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class TaskPatch(
    /** Identifier of the task to change. */
    val id: Long,
    /** New title. Cannot be empty. */
    val title: String? = null,
    /** New detail. Pass an empty string to clear it. */
    val notes: String? = null,
    /** New priority: "high", "medium", "low" or "none". */
    val priority: String? = null,
    /** New due date. */
    val dueDateTime: LocalDateTime? = null,
    /** New all-day flag for the due date. Omit to keep the task's current all-day-ness. */
    val dueAllDay: Boolean? = null,
    /** New start date. */
    val startDateTime: LocalDateTime? = null,
    /** New all-day flag for the start date. Omit to keep it as it is. */
    val startAllDay: Boolean? = null,
    /** New recurrence rule, or an empty string to stop the task repeating. */
    val recurrence: String? = null,
    /** What the next occurrence is measured from: "due_date" or "completion_date". */
    val repeatFrom: String? = null,
    /** Move the task, and its subtasks, to this list. */
    val listId: Long? = null,
    /** Re-parent the task. Pass 0 to make it top-level. */
    val parentId: Long? = null,
    /** File the task at this place. Pass 0 to remove the place and its location reminders. */
    val placeId: Long? = null,
    /** True to remove the task's due date. Cannot be combined with dueDateTime. */
    val clearDueDate: Boolean? = null,
    /** True to remove the task's start date. Cannot be combined with startDateTime. */
    val clearStartDate: Boolean? = null,
)

/** A list that tasks belong to. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppTaskList(
    /** Identifier of this list on this device. */
    val id: Long,
    /** What the list is called. */
    val title: String,
    /** One of "owner", "read_write" or "read_only". */
    val access: String,
    /** True when tasks on this list cannot be changed. */
    val isReadOnly: Boolean,
    /** Identifier of the account that owns the list. */
    val accountId: Long? = null,
    /** The list's colour as an ARGB integer, when it has one. */
    val color: Int? = null,
    /** The list's icon name, when it has one. */
    val icon: String? = null,
)

/** A page of lists. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppTaskListPage(
    /** The lists in this page. */
    val lists: List<AppTaskList>,
    /** How many lists exist in total, ignoring limit and offset. */
    val total: Int,
    /** The offset this page starts at, or null for a count: a limit of zero asks how many. */
    val offset: Int?,
    /** True when more lists exist than were returned. */
    val hasMore: Boolean?,
)

/** A tag. A task carries any number of tags. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppTag(
    /** Identifier of this tag on this device. */
    val id: Long,
    /** What the tag is called. */
    val name: String,
    /** The tag's colour as an ARGB integer, when it has one. */
    val color: Int? = null,
    /** The tag's icon name, when it has one. */
    val icon: String? = null,
)

/** A page of tags. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppTagPage(
    /** The tags in this page. */
    val tags: List<AppTag>,
    /** How many tags exist in total, ignoring limit and offset. */
    val total: Int,
    /** The offset this page starts at, or null for a count: a limit of zero asks how many. */
    val offset: Int?,
    /** True when more tags exist than were returned. */
    val hasMore: Boolean?,
)

/** What one task's tags gained and lost. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppTagEdit(
    /** Identifier of the task. */
    val taskId: Long,
    /** How many tags went on it. A tag it already carried is not counted. */
    val added: Int,
    /** How many tags came off it. A tag it was not carrying is not counted. */
    val removed: Int,
)

/** The result of adding and removing tags across tasks. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppTagChange(
    /** The tasks as they are after the change. */
    val tasks: List<AppTask>,
    /** How many tags went on across every task. */
    val added: Int,
    /** How many tags came off across every task. */
    val removed: Int,
    /** What changed on each task, in the order the tasks were sent. */
    val edits: List<AppTagEdit>,
)

/** The result of adding and removing reminders on a task. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppReminderChange(
    /** Every reminder on the task afterwards, both kinds. */
    val reminders: List<AppReminder>,
    /**
     * Identifiers of the reminders that were added. A reminder the task already had comes back
     * here with its existing identifier rather than being duplicated.
     */
    val addedReminderIds: LongArray,
    /** How many reminders were deleted. An identifier that matched nothing is not counted. */
    val removed: Int,
    /**
     * The task as it is afterwards. Adding the first location reminder files the task at that
     * place, so its placeId can change here without being asked for.
     */
    val task: AppTask? = null,
)

/** A saved place, used for location reminders and for filing a task somewhere. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppPlace(
    /** Identifier of this place on this device. */
    val id: Long,
    /** What the place is called, for example "Home". Falls back to the display name. */
    val name: String,
    /** The address or label the place was saved under. */
    val displayName: String,
    /** Street address, when known. */
    val address: String? = null,
    /** Phone number, when known. */
    val phone: String? = null,
    /** Web address, when known. */
    val url: String? = null,
    /** Latitude in degrees. */
    val latitude: Double,
    /** Longitude in degrees. */
    val longitude: Double,
    /** Geofence radius in metres. */
    val radius: Int,
    /** The place's colour as an ARGB integer, when it has one. */
    val color: Int? = null,
    /** The place's icon name, when it has one. */
    val icon: String? = null,
)

/** A page of places. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppPlacePage(
    /** The places in this page. */
    val places: List<AppPlace>,
    /** How many places exist in total, ignoring limit and offset. */
    val total: Int,
    /** The offset this page starts at, or null for a count: a limit of zero asks how many. */
    val offset: Int?,
    /** True when more places exist than were returned. */
    val hasMore: Boolean?,
)

/**
 * A reminder on a task. One collection covers both kinds: a time reminder carries a trigger time
 * or an offset, a location reminder carries a place and no timing.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppReminder(
    /** Identifier of this reminder on this device. */
    val id: Long,
    /** Identifier of the task the reminder belongs to. */
    val taskId: Long,
    /**
     * One of "date_time", "relative_start", "relative_due", "random", "snooze",
     * "location_arrival" or "location_departure".
     */
    val type: String,
    /** When the reminder fires, for a reminder set to a fixed time. */
    val triggerAt: LocalDateTime? = null,
    /** Offset from the start or due date in milliseconds, for a relative reminder. */
    val offsetMs: Long? = null,
    /** The offset as a person would say it, for example "1 hour before due". */
    val offsetDescription: String? = null,
    /** How many times the reminder repeats, when it repeats. */
    val repeatCount: Int? = null,
    /** Gap between repeats in milliseconds, when it repeats. */
    val intervalMs: Long? = null,
    /** Identifier of the place, for a location reminder. */
    val placeId: Long? = null,
)

/** A reminder to add to a task. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class NewReminder(
    /**
     * One of "date_time", "relative_start", "relative_due", "random", "snooze",
     * "location_arrival" or "location_departure". A "snooze" quiets the task's other reminders
     * until it fires, then deletes itself - it is how "don't remind me about this until tomorrow"
     * is done.
     */
    val type: String,
    /** When the reminder fires. For "date_time" and "snooze" only. */
    val triggerAt: LocalDateTime? = null,
    /**
     * Signed offset from the start or due date in milliseconds; negative is before. For "random",
     * how often it fires, at least a minute. For "relative_start", "relative_due" and "random"
     * only.
     */
    val offsetMs: Long? = null,
    /** How many times the reminder repeats after the first trigger. Needs intervalMs. */
    val repeatCount: Int? = null,
    /** Gap between repeats in milliseconds, at least a minute. */
    val intervalMs: Long? = null,
    /**
     * Identifier of the place that triggers it. Required for "location_arrival" and
     * "location_departure", and not allowed on any other kind.
     */
    val placeId: Long? = null,
)

/** A page of reminders. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppReminderPage(
    /** The reminders in this page. */
    val reminders: List<AppReminder>,
    /** How many reminders match in total, ignoring limit and offset. */
    val total: Int,
    /** The offset this page starts at, or null for a count: a limit of zero asks how many. */
    val offset: Int?,
    /** True when more reminders match than were returned. */
    val hasMore: Boolean?,
)

/** A sync account. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppAccount(
    /** Identifier of this account on this device. */
    val id: Long,
    /** What the account is called. */
    val name: String? = null,
    /** Account type, for example "caldav", "tasks_org", "google_tasks", "microsoft" or "local". */
    val type: String,
    /** The signed-in user, when the account has one. */
    val username: String? = null,
    /** The server address, for a self-hosted account. */
    val url: String? = null,
    /**
     * The last sync error, when there is one: "unauthorized", "payment_required",
     * "terms_required" or "failed". Null when the account is healthy.
     */
    val error: String? = null,
    /** True when the server advances recurring tasks itself. */
    val repeatsOnServer: Boolean,
)

/** A page of accounts. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppAccountPage(
    /** The accounts in this page. */
    val accounts: List<AppAccount>,
    /** How many accounts exist in total, ignoring limit and offset. */
    val total: Int,
    /** The offset this page starts at, or null for a count: a limit of zero asks how many. */
    val offset: Int?,
    /** True when more accounts exist than were returned. */
    val hasMore: Boolean?,
)

internal fun Long.asLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

internal fun LocalDateTime.asEpochMillis(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun Long?.dateTime(): LocalDateTime? = this?.asLocalDateTime()

internal fun ApiRow.toAppTask(): AppTask = toTaskRow().toAppTask()

internal fun TaskRow.toAppTask() = AppTask(
    id = id,
    title = title,
    notes = notes,
    priority = priority,
    dueDateTime = due.dateTime(),
    dueAllDay = dueAllDay,
    startDateTime = start.dateTime(),
    startAllDay = startAllDay,
    completedAt = completed.dateTime(),
    createdAt = created.dateTime(),
    modifiedAt = modified.dateTime(),
    recurrence = recurrence,
    repeatFrom = repeatFrom,
    parentId = parentId,
    listId = listId,
    placeId = placeId,
    tagIds = tagIds.toLongArray(),
    childCount = childCount,
    uncompletedChildCount = uncompletedChildCount,
    isReadOnly = isReadOnly,
)

internal fun ApiRow.toAppTaskList(): AppTaskList = toListRow().toAppTaskList()

internal fun ListRow.toAppTaskList() = AppTaskList(
    id = id,
    title = title,
    access = access,
    isReadOnly = isReadOnly,
    accountId = accountId,
    color = color,
    icon = icon,
)

/** The result of creating a tag. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppTagCreation(
    /** The tag, whether it was just created or already existed. */
    val tag: AppTag,
    /**
     * False when a tag of that name already existed and was returned instead. Nothing was
     * written, and the colour and icon sent were ignored.
     */
    val created: Boolean,
)

/** The result of creating a place. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppPlaceCreation(
    /** The place, whether it was just created or already existed. */
    val place: AppPlace,
    /**
     * False when a place at those coordinates already existed and was returned instead.
     * Nothing was written, and the other details sent were ignored.
     */
    val created: Boolean,
)

internal fun Created<TagRow>.toAppTagCreation() = AppTagCreation(row.toAppTag(), created)

internal fun Created<PlaceRow>.toAppPlaceCreation() = AppPlaceCreation(row.toAppPlace(), created)

internal fun ApiRow.toAppTag(): AppTag = toTagRow().toAppTag()

internal fun TagRow.toAppTag() = AppTag(id = id, name = name, color = color, icon = icon)

internal fun ApiRow.toAppPlace(): AppPlace = toPlaceRow().toAppPlace()

internal fun PlaceRow.toAppPlace() = AppPlace(
    id = id,
    name = label,
    displayName = displayName,
    address = address,
    phone = phone,
    url = url,
    latitude = latitude,
    longitude = longitude,
    radius = radius,
    color = color,
    icon = icon,
)

internal fun ApiRow.toAppReminder(): AppReminder = toReminderRow().toAppReminder()

internal fun ReminderRow.toAppReminder() = AppReminder(
    id = id,
    taskId = taskId,
    type = type,
    triggerAt = triggerAt.dateTime(),
    offsetMs = offsetMs,
    offsetDescription = offsetMs?.let { describeOffset(it, type) },
    repeatCount = repeatCount,
    intervalMs = intervalMs,
    placeId = placeId,
)

internal fun ApiRow.toAppAccount(): AppAccount = toAccountRow().toAppAccount()

internal fun AccountRow.toAppAccount() = AppAccount(
    id = id,
    name = name,
    type = type,
    username = username,
    url = url,
    error = error,
    repeatsOnServer = repeatsOnServer,
)

internal fun NewTask.toTaskWrite() = TaskWrite(
    title = title,
    notes = notes,
    priority = priority,
    due = dueDateTime?.asEpochMillis(),
    dueAllDay = dueAllDay,
    start = startDateTime?.asEpochMillis(),
    startAllDay = startAllDay,
    recurrence = recurrence,
    repeatFrom = repeatFrom,
    parentId = parentId,
    listId = listId,
    placeId = placeId,
)

internal fun TaskPatch.toTaskWrite(): TaskWrite {
    require(clearDueDate != true || dueDateTime == null) {
        "Send either dueDateTime or clearDueDate, not both."
    }
    require(clearStartDate != true || startDateTime == null) {
        "Send either startDateTime or clearStartDate, not both."
    }
    return TaskWrite(
        title = title,
        notes = notes,
        priority = priority,
        due = if (clearDueDate == true) 0L else dueDateTime?.asEpochMillis(),
        dueAllDay = dueAllDay,
        start = if (clearStartDate == true) 0L else startDateTime?.asEpochMillis(),
        startAllDay = startAllDay,
        recurrence = recurrence,
        repeatFrom = repeatFrom,
        parentId = parentId,
        listId = listId,
        placeId = placeId,
    )
}

internal fun Deletion.toAppDeletion() = AppDeletion(rowsDeleted > 0, alsoAffected)

internal fun NewReminder.toReminderWrite() = ReminderWrite(
    type = type,
    triggerAt = triggerAt?.asEpochMillis(),
    offsetMs = offsetMs,
    repeatCount = repeatCount,
    intervalMs = intervalMs,
    placeId = placeId,
)

internal fun Creation.toAppTaskCreation() = AppTaskCreation(
    tasks = tasks.map { it.toAppTask() },
    movedToParentListTaskIds = movedToParentListIds.toLongArray(),
)

internal fun Revision.toAppTaskUpdate() = AppTaskUpdate(
    tasks = tasks.map { it.toAppTask() },
    unchangedTaskIds = unchangedIds.toLongArray(),
    movedToParentListTaskIds = movedToParentListIds.toLongArray(),
    detachedTaskIds = detachedIds.toLongArray(),
)

internal fun Completion.toAppCompletion() = AppCompletion(
    tasks = tasks.map { it.toAppTask() },
    advancedTaskIds = advancedTaskIds.toLongArray(),
    unchangedTaskIds = unchangedIds.toLongArray(),
    alsoCompletedTaskIds = alsoCompletedTaskIds.toLongArray(),
    reopenedTaskIds = reopenedTaskIds.toLongArray(),
)

internal fun TagChange.toAppTagChange() = AppTagChange(
    tasks = tasks.map { it.toAppTask() },
    added = added,
    removed = removed,
    edits = edits.map { AppTagEdit(taskId = it.taskId, added = it.added, removed = it.removed) },
)

internal fun ReminderEdit.toAppReminderChange() = AppReminderChange(
    reminders = reminders.map { it.toAppReminder() },
    addedReminderIds = addedIds.toLongArray(),
    removed = removed,
    task = task?.toAppTask(),
)
