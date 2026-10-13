@file:OptIn(ExperimentalSerializationApi::class)

package org.tasks.sync.todoist

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import org.tasks.data.Redacted

@Serializable
data class TodoistItem(
    val id: String,
    @Redacted val content: String = "",
    @Redacted val description: String = "",
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("section_id") val sectionId: String? = null,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("child_order") val childOrder: Int = 0,
    val priority: Int = PRIORITY_DEFAULT,
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
