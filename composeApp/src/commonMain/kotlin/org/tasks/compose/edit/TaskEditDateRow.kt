package org.tasks.compose.edit

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun TaskEditDateRow(
    value: String,
    overdue: Boolean,
    isEmpty: Boolean,
    missingDate: Boolean,
    title: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isError = overdue || missingDate
    TaskEditCardRow(
        value = value,
        valueColor = when {
            isError -> MaterialTheme.colorScheme.error
            isEmpty -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurface
        },
        onClick = onClick,
        modifier = modifier,
        title = title,
        icon = icon,
        iconTint = if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}
