package org.tasks.compose.chips

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.tasks.kmp.formatNumber
import org.tasks.themes.TasksIcons
import org.tasks.themes.chipColors

@Composable
fun SubtaskChip(
    collapsed: Boolean,
    children: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
    chipColor: Color = defaultChipColor(),
) {
    Chip(
        icon = if (collapsed)
            TasksIcons.KEYBOARD_ARROW_DOWN
        else
            TasksIcons.KEYBOARD_ARROW_UP,
        text = formatNumber(children),
        color = chipColor,
        onClick = onClick,
        enabled = enabled,
    )
}

@Composable
internal fun defaultChipColor(): Color =
    MaterialTheme.colorScheme.surfaceContainerHighest
