/**
 * Checkbox.kt — Task completion checkbox for the Wear OS task list.
 *
 * Renders a tappable icon that reflects the current completion state of a task:
 * - **Completed** → filled check box icon
 * - **Repeating** → repeat icon (to signal recurring tasks)
 * - **Incomplete** → empty check box outline
 *
 * The icon color is derived from the task's priority using [ColorProvider.priorityColor]
 * (higher priority → more vivid color).
 *
 * Used inside [TaskRow] as the leading icon of each task card.
 */
package org.tasks.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import org.tasks.kmp.org.tasks.themes.ColorProvider

/**
 * Tappable checkbox icon showing the task's completion state.
 *
 * @param completed      Whether the task is marked as done.
 * @param repeating      Whether the task is recurring.
 * @param priority       Task priority (0–3); determines icon tint color.
 * @param toggleComplete Callback invoked when the user taps the checkbox.
 */
@Composable
fun Checkbox(
    completed: Boolean,
    repeating: Boolean,
    priority: Int,
    toggleComplete: () -> Unit,
) {
    Button(
        onClick = { toggleComplete() },
        colors = ButtonDefaults.iconButtonColors(),
    ) {
        Icon(
            imageVector = when {
                completed -> Icons.Outlined.CheckBox
                repeating -> Icons.Outlined.Repeat
                else -> Icons.Outlined.CheckBoxOutlineBlank
            },
            tint = Color(
                ColorProvider.priorityColor(priority, isDarkMode = true)
            ),
            contentDescription = null,
        )
    }

}