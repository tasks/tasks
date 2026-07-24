package org.tasks.compose.edit

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun TaskEditDateRow(
    value: String,
    overdue: Boolean,
    isEmpty: Boolean,
    title: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TaskEditCardRow(
        value = value,
        valueColor = when {
            overdue -> MaterialTheme.colorScheme.error
            // TODO: tint when date empty but there is a reminder set
            isEmpty -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurface
        },
        onClick = onClick,
        modifier = modifier,
        title = title,
        icon = icon,
        iconTint = if (overdue) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}
