package org.tasks.compose.chips

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.tasks.themes.TasksIcons
import org.tasks.themes.chipColors

@Composable
fun SubtaskChip(
    collapsed: Boolean,
    children: Int,
    compact: Boolean,
    onClick: () -> Unit,
    chipColor: Color = defaultChipColor(),
) {
    Chip(
        icon = if (collapsed)
            TasksIcons.KEYBOARD_ARROW_DOWN
        else
            TasksIcons.KEYBOARD_ARROW_UP,
        text = children.toString(),
        color = chipColor,
        onClick = onClick,
    )
}

@Composable
internal fun defaultChipColor(): Color =
    MaterialTheme.colorScheme.surfaceContainerHighest
