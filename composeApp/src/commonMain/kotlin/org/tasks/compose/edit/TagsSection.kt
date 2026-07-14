package org.tasks.compose.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.chips.Chip
import org.tasks.compose.chips.ChipGroup
import org.tasks.data.entity.TagData
import org.tasks.themes.TasksIcons
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.add_tag
import tasks.kmp.generated.resources.drawer_tags

@Composable
fun TagsSection(
    tags: List<TagData>,
    colorProvider: (Int) -> Int,
    onClick: () -> Unit,
    onClear: (TagData) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Text(
                text = stringResource(Res.string.drawer_tags).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            ChipGroup {
                tags.sortedBy(TagData::name).forEach { tag ->
                    Chip(
                        icon = tag.icon ?: TasksIcons.LABEL,
                        name = tag.name,
                        theme = tag.color ?: 0,
                        showText = true,
                        showIcon = true,
                        onClick = onClick,
                        colorProvider = colorProvider,
                        clear = { onClear(tag) },
                    )
                }
                AddTagChip(onClick = onClick)
            }
        }
    }
}

@Composable
private fun AddTagChip(onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.small
    val borderColor = MaterialTheme.colorScheme.outline
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified
    ) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = Color.Transparent,
            modifier = Modifier
                .defaultMinSize(minHeight = 26.dp)
                .dashedBorder(color = borderColor, shape = shape),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor,
                )
                Text(
                    text = stringResource(Res.string.add_tag),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                )
            }
        }
    }
}

private fun Modifier.dashedBorder(
    color: Color,
    shape: Shape,
    strokeWidth: Dp = 1.5.dp,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 4.dp,
): Modifier = drawWithCache {
    val strokePx = strokeWidth.toPx()
    val inset = strokePx / 2f
    val stroke = Stroke(
        width = strokePx,
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashLength.toPx(), gapLength.toPx())
        ),
    )
    val outline = shape.createOutline(
        Size(size.width - strokePx, size.height - strokePx),
        layoutDirection,
        this,
    )
    onDrawBehind {
        translate(left = inset, top = inset) {
            drawOutline(outline = outline, color = color, style = stroke)
        }
    }
}
