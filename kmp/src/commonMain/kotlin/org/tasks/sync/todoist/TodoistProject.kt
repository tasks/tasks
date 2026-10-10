@file:OptIn(ExperimentalSerializationApi::class)

package org.tasks.sync.todoist

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import org.tasks.data.Redacted
import org.tasks.data.entity.CaldavCalendar

/**
 * A Todoist project, which maps onto a [CaldavCalendar].
 *
 * Todoist projects nest (via [parentId]) and contain sections, neither of which Tasks
 * models. Nesting is flattened -- every project becomes a top-level list -- and
 * [org.tasks.sync.todoist.TodoistItem.sectionId] is preserved on round-trip so that
 * editing a task from Tasks doesn't move it out of its section.
 */
@Serializable
data class TodoistProject(
    val id: String,
    @Redacted
    @JsonNames("name")
    val name: String = "",
    /** Todoist palette name, e.g. "berry_red". Not a hex value. */
    val color: String? = null,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("child_order") val childOrder: Int = 0,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("is_shared") val isShared: Boolean = false,
    /**
     * The Inbox. Sync calls this `inbox_project`, some REST responses
     * `is_inbox_project`.
     */
    @SerialName("inbox_project")
    @JsonNames("is_inbox_project")
    val isInboxProject: Boolean = false,
) {
    fun applyTo(list: CaldavCalendar) {
        with(list) {
            name = this@TodoistProject.name
            url = this@TodoistProject.id
            uuid = this@TodoistProject.id
            access = when {
                isShared -> CaldavCalendar.ACCESS_READ_WRITE
                else -> CaldavCalendar.ACCESS_OWNER
            }
            this@TodoistProject.color
                ?.let { TodoistColors.toArgb(it) }
                ?.let { color = it }
        }
    }
}

@Serializable
data class TodoistSection(
    val id: String,
    @Redacted val name: String = "",
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("section_order") val sectionOrder: Int = 0,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("is_archived") val isArchived: Boolean = false,
)

@Serializable
data class TodoistLabel(
    val id: String,
    @Redacted val name: String = "",
    val color: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)
