/**
 * TaskLite.kt — Lightweight read-only model exposed to the Wear UI layer.
 *
 * [TaskLite] is the UI-facing projection of [TaskEntity]. It strips sync-
 * related metadata (dirty, syncedAt, phoneTaskId, per-field timestamps)
 * and adds a [stringId] field used as a stable key in [ScalingLazyColumn].
 *
 * Conversion: [TaskEntity.toTaskLite()] (extension in [TaskRepository]).
 */
package org.tasks.presentation.model

/**
 * Lightweight task model for the Wear app.
 * Will be replaced by Room entity in the future.
 */
data class TaskLite(
    val id: Long,
    val stringId: String = "", // Original string ID for unique keys
    val title: String,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val timestamp: String? = null,
    val completed: Boolean = false,
    val hidden: Boolean = false,
    val numSubtasks: Int = 0,
    val collapsed: Boolean = false,
    val indent: Int = 0,
    val repeating: Boolean = false,
    val priority: Int = 0,
    // Date/time and reminder fields
    val dueDate: Long? = null,
    val dueTime: Long? = null,
    val reminder: Boolean = false,
    val reminderTime: Long? = null,
)
