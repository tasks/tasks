package org.tasks.mcp

import org.tasks.api.AccountRow
import org.tasks.api.ListRow
import org.tasks.api.PlaceRow
import org.tasks.api.ReminderRow
import org.tasks.api.TagRow
import org.tasks.api.TaskRow
import org.tasks.api.describeOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class ApiTask(
    val id: Long,
    val title: String,
    val notes: String? = null,
    val priority: String,
    val due: Timestamp? = null,
    @SerialName("due_all_day") val dueAllDay: Boolean = false,
    val start: Timestamp? = null,
    @SerialName("start_all_day") val startAllDay: Boolean = false,
    val completed: Timestamp? = null,
    val created: Timestamp? = null,
    val modified: Timestamp? = null,
    val recurrence: String? = null,
    @SerialName("repeat_from") val repeatFrom: String? = null,
    @SerialName("parent_id") val parentId: Long? = null,
    @SerialName("list_id") val listId: Long? = null,
    @SerialName("tag_ids") val tagIds: List<Long> = emptyList(),
    @SerialName("place_id") val placeId: Long? = null,
    @SerialName("child_count") val childCount: Int = 0,
    @SerialName("uncompleted_child_count") val uncompletedChildCount: Int = 0,
    @SerialName("is_read_only") val isReadOnly: Boolean = false,
)

@Serializable
data class Timestamp(
    val millis: Long,
    val iso: String,
)

@Serializable
data class ApiReminder(
    val id: Long,
    @SerialName("task_id") val taskId: Long,
    val type: String,
    @SerialName("trigger_at") val triggerAt: Timestamp? = null,
    @SerialName("offset_ms") val offsetMs: Long? = null,

    @SerialName("offset_description") val offsetDescription: String? = null,
    @SerialName("repeat_count") val repeatCount: Int? = null,
    @SerialName("interval_ms") val intervalMs: Long? = null,

    @SerialName("place_id") val placeId: Long? = null,
)

@Serializable
data class ApiList(
    val id: Long,
    val title: String,
    val color: Int? = null,
    val icon: String? = null,
    val access: String,
    @SerialName("account_id") val accountId: Long? = null,
    @SerialName("is_read_only") val isReadOnly: Boolean = false,
)

@Serializable
data class ApiTag(
    val id: Long,
    val name: String,
    val color: Int? = null,
    val icon: String? = null,
)

@Serializable
data class ApiPlace(
    val id: Long,
    val name: String,
    @SerialName("display_name") val displayName: String,
    val address: String? = null,
    val phone: String? = null,
    val url: String? = null,
    val latitude: Double,
    val longitude: Double,
    val radius: Int,
    val color: Int? = null,
    val icon: String? = null,
)

@Serializable
data class ApiAccount(
    val id: Long,
    val name: String? = null,
    val type: String,
    val username: String? = null,
    val url: String? = null,

    val error: String? = null,

    @SerialName("repeats_on_server") val repeatsOnServer: Boolean = false,
)

private val ISO: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.systemDefault())

fun Long?.toTimestamp(): Timestamp? =
    this?.let { Timestamp(it, ISO.format(Instant.ofEpochMilli(it))) }

internal fun TaskRow.toApiTask() = ApiTask(
    id = id,
    title = title,
    notes = notes,
    priority = priority,
    due = due.toTimestamp(),
    dueAllDay = dueAllDay,
    start = start.toTimestamp(),
    startAllDay = startAllDay,
    completed = completed.toTimestamp(),
    created = created.toTimestamp(),
    modified = modified.toTimestamp(),
    recurrence = recurrence,
    repeatFrom = repeatFrom,
    parentId = parentId,
    listId = listId,
    tagIds = tagIds,
    placeId = placeId,
    childCount = childCount,
    uncompletedChildCount = uncompletedChildCount,
    isReadOnly = isReadOnly,
)

internal fun ReminderRow.toApiReminder() = ApiReminder(
    id = id,
    taskId = taskId,
    type = type,
    placeId = placeId,
    triggerAt = triggerAt.toTimestamp(),
    offsetMs = offsetMs,
    offsetDescription = offsetMs?.let { describeOffset(it, type) },
    repeatCount = repeatCount,
    intervalMs = intervalMs,
)

internal fun ListRow.toApiList() = ApiList(
    id = id,
    title = title,
    color = color,
    icon = icon,
    access = access,
    accountId = accountId,
    isReadOnly = isReadOnly,
)

internal fun TagRow.toApiTag() = ApiTag(id = id, name = name, color = color, icon = icon)

internal fun PlaceRow.toApiPlace() = ApiPlace(
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

internal fun AccountRow.toApiAccount() = ApiAccount(
    id = id,
    name = name,
    type = type,
    username = username,
    url = url,
    error = error,
    repeatsOnServer = repeatsOnServer,
)
