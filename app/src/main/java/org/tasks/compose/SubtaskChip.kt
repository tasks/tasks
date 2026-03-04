package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import org.tasks.R
import org.tasks.themes.TasksIcons
import org.tasks.themes.chipColors
import java.text.NumberFormat

@Composable
fun SubtaskChip(
    collapsed: Boolean,
    children: Int,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val colors = chipColors(colorResource(org.tasks.kmp.R.color.grey_300).toArgb())
    Chip(
        icon = if (collapsed)
            TasksIcons.KEYBOARD_ARROW_DOWN
        else
            TasksIcons.KEYBOARD_ARROW_UP,
        text = if (compact)
            NumberFormat.getInstance().format(children)
        else
            pluralStringResource(R.plurals.subtask_count, children, children),
        color = Color(colors.backgroundColor),
        onClick = onClick,
    )
}
