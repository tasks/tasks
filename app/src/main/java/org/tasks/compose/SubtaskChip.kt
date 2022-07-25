package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.tasks.R
import org.tasks.data.TaskContainer
import java.text.NumberFormat

@Composable
fun SubtaskChip(
    task: TaskContainer,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Chip(
        icon = if (task.isCollapsed)
            R.drawable.ic_keyboard_arrow_down_black_24dp
        else
            R.drawable.ic_keyboard_arrow_up_black_24dp,
        name = if (compact)
            NumberFormat.getInstance().format(task.children)
        else
            context.resources.getQuantityString(R.plurals.subtask_count, task.children, task.children),
        theme = 0,
        showText = true,
        showIcon = true,
        onClick = onClick,
        colorProvider = { context.getColor(R.color.default_chip_background) },
    )
}
