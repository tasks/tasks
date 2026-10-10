@file:OptIn(ExperimentalSerializationApi::class)

package org.tasks.sync.todoist

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import org.tasks.data.Redacted

/**
 * A Todoist task ("item" in Sync API terms).
 *
 * Field names differ between the Sync endpoint and the REST endpoints for the same
 * concepts (`checked` vs `is_completed`, `added_at` vs `created_at`), so the
 * divergent ones carry [JsonNames] aliases and this type deserializes from either.
 */
@Serializable
data class TodoistItem(
    val id: String,
    @Redacted val content: String = "",
    @Redacted val description: String = "",
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("section_id") val sectionId: String? = null,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("child_order") val childOrder: Int = 0,
    /** 1 (default) through 4 (urgent) -- inverted relative to [org.tasks.data.entity.Task.Priority]. */
    val priority: Int = PRIORITY_DEFAULT,
    /** Label *names*, not ids. */
    val labels: List<String> = emptyList(),
    val due: TodoistDue? = null,
    val deadline: TodoistDeadline? = null,
    @JsonNames("is_completed")
    val checked: Boolean = false,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("added_at")
    @JsonNames("created_at")
    val addedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    companion object {
        const val PRIORITY_DEFAULT = 1
        const val PRIORITY_URGENT = 4
    }
}

/**
 * Todoist due dates come in three shapes, distinguished by the format of [date]:
 * a floating date (`2026-10-09`), a floating datetime (`2026-10-09T15:00:00`), or a
 * fixed-zone datetime (`2026-10-09T15:00:00Z` plus [timezone]).
 *
 * [string] is the human-authored natural-language form ("every 2 weeks on mon"). It is
 * authoritative for recurrence -- Todoist has no RRULE -- so it is round-tripped
 * verbatim whenever we can't confidently translate it.
 */
@Serializable
data class TodoistDue(
    val date: String,
    @Redacted val string: String = "",
    val lang: String = "en",
    @SerialName("is_recurring") val isRecurring: Boolean = false,
    val timezone: String? = null,
)

@Serializable
data class TodoistDeadline(
    val date: String,
    val lang: String = "en",
)
