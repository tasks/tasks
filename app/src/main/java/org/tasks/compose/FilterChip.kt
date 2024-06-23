package org.tasks.compose

import androidx.compose.runtime.Composable
import org.tasks.filters.Filter

@Composable
fun FilterChip(
    filter: Filter,
    defaultIcon: String,
    showText: Boolean,
    showIcon: Boolean,
    onClick: (Filter) -> Unit,
    colorProvider: (Int) -> Int,
) {
    Chip(
        filter.icon ?: defaultIcon,
        filter.title,
        filter.tint,
        showText,
        showIcon,
        onClick = { onClick(filter) },
        colorProvider = colorProvider,
    )
}
