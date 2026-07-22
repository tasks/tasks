/**
 * TaskRow.kt — Convenience composable that wires a [Checkbox] into a [TaskCard].
 *
 * This is the composable used by [OfflineTaskListScreen] to render each task
 * in the scrollable list. It maps the [TaskLite] model to the visual [TaskCard]
 * and handles the three user interactions:
 *
 * 1. **Tap the card** → opens the task for editing ([onClick]).
 * 2. **Tap the checkbox** → toggles completion ([onToggleComplete]).
 * 3. **Tap the subtask button** → collapses/expands subtasks ([onToggleSubtasks]).
 */
package org.tasks.presentation.components

import androidx.compose.runtime.Composable
import org.tasks.presentation.model.TaskLite

/**
 * A row component for displaying a task in a list.
 * Combines TaskCard with a Checkbox for task completion.
 */
@Composable
fun TaskRow(
    task: TaskLite,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onToggleSubtasks: () -> Unit,
) {
    TaskCard(
        text = task.title,
        timestamp = task.timestamp,
        hidden = task.hidden,
        numSubtasks = task.numSubtasks,
        subtasksCollapsed = task.collapsed,
        toggleSubtasks = onToggleSubtasks,
        icon = {
            Checkbox(
                completed = task.completed,
                repeating = task.repeating,
                priority = task.priority,
                toggleComplete = onToggleComplete,
            )
        },
        onClick = onClick,
    )
}
