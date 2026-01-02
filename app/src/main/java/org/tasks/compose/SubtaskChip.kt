package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import org.tasks.R
import org.tasks.themes.TasksIcons
import java.text.NumberFormat

@Composable
fun SubtaskChip(
    collapsed: Boolean,
    children: Int,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val chipColor = colorResource(R.color.default_chip_background).toArgb()
    Chip(
        icon = if (collapsed)
            TasksIcons.KEYBOARD_ARROW_DOWN
        else
            TasksIcons.KEYBOARD_ARROW_UP,
        name = if (compact)
            NumberFormat.getInstance().format(children)
        else
            pluralStringResource(R.plurals.subtask_count, children, children),
        theme = 0,
        showText = true,
        showIcon = true,
        onClick = onClick,
        colorProvider = { chipColor },
    )
}
