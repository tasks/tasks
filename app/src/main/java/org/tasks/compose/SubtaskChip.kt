package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.tasks.R
import java.text.NumberFormat

@Composable
fun SubtaskChip(
    collapsed: Boolean,
    children: Int,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Chip(
        icon = if (collapsed)
            R.drawable.ic_keyboard_arrow_down_black_24dp
        else
            R.drawable.ic_keyboard_arrow_up_black_24dp,
        name = if (compact)
            NumberFormat.getInstance().format(children)
        else
            remember(children) {
                context.resources.getQuantityString(R.plurals.subtask_count, children, children)
            },
        theme = 0,
        showText = true,
        showIcon = true,
        onClick = onClick,
        colorProvider = { context.getColor(R.color.default_chip_background) },
    )
}
