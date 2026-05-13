package org.tasks.compose.chips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.tasks.compose.components.imageVectorByName
import org.tasks.filters.Filter
import org.tasks.themes.contentColor

@Composable
fun Chip(
    icon: String?,
    name: String?,
    theme: Int,
    showText: Boolean,
    showIcon: Boolean,
    onClick: () -> Unit,
    colorProvider: (Int) -> Int,
    clear: (() -> Unit)? = null,
) {
    Chip(
        color = remember(theme) { Color(colorProvider(theme)) },
        text = if (showText) name else null,
        icon = if (showIcon && icon != null) icon else null,
        onClick = onClick,
        clear = clear,
    )
}

@Composable
fun Chip(
    text: String? = null,
    icon: String? = null,
    color: Color,
    onClick: () -> Unit = {},
    clear: (() -> Unit)? = null,
) {
    val onColor = Color(contentColor(color.toArgb()))
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified
    ) {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            color = color,
            modifier = Modifier.defaultMinSize(minHeight = 26.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                icon?.let {
                    imageVectorByName(it)?.let { vector ->
                        Icon(
                            imageVector = vector,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = onColor,
                        )
                    }
                }
                text?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = onColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                clear?.let { onClearClick ->
                    Icon(
                        imageVector = Icons.Outlined.Cancel,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onClearClick() },
                        contentDescription = null,
                        tint = onColor,
                    )
                }
            }
        }
    }
}

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        content()
    }
}
