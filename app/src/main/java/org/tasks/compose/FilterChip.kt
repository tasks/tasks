package org.tasks.compose

import androidx.compose.runtime.Composable
import com.todoroo.astrid.api.Filter
import org.tasks.themes.CustomIcons

@Composable
fun FilterChip(
    filter: Filter,
    defaultIcon: Int,
    showText: Boolean,
    showIcon: Boolean,
    onClick: (Filter) -> Unit,
    colorProvider: (Int) -> Int,
) {
    Chip(
        CustomIcons.getIcon(filter.icon, defaultIcon),
        filter.title,
        filter.tint,
        showText,
        showIcon,
        onClick = { onClick(filter) },
        colorProvider = colorProvider,
    )
}
