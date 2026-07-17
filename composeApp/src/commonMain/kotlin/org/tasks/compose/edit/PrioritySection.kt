package org.tasks.compose.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Flag as OutlinedFlag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.priorityColor
import org.tasks.data.entity.Task
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.none
import tasks.kmp.generated.resources.priority_high
import tasks.kmp.generated.resources.priority_low
import tasks.kmp.generated.resources.priority_medium
import tasks.kmp.generated.resources.sort_priority

private val PRIORITY_ORDER = listOf(
    Task.Priority.NONE,
    Task.Priority.LOW,
    Task.Priority.MEDIUM,
    Task.Priority.HIGH,
)

private val CARD_SPACING = 8.dp
private val CARD_HORIZONTAL_PADDING = 4.dp

@Composable
fun PrioritySection(
    priority: Int,
    onPriorityChange: (Int) -> Unit,
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
                text = stringResource(Res.string.sort_priority).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            val labels = listOf(
                stringResource(Res.string.none),
                stringResource(Res.string.priority_low),
                stringResource(Res.string.priority_medium),
                stringResource(Res.string.priority_high),
            ).map { it.uppercase() }
            val labelStyle = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            )
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            BoxWithConstraints {
                val availableWidth = maxWidth
                val showLabels = remember(availableWidth, labels, labelStyle, density) {
                    val cardWidthPx = with(density) {
                        ((availableWidth - CARD_SPACING * (PRIORITY_ORDER.size - 1)) /
                                PRIORITY_ORDER.size).toPx()
                    }
                    val textBudgetPx = with(density) {
                        cardWidthPx - (CARD_HORIZONTAL_PADDING * 2 + 2.dp).toPx()
                    }
                    labels.all { label ->
                        measurer.measure(label, labelStyle).size.width <= textBudgetPx
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(CARD_SPACING)) {
                    PRIORITY_ORDER.forEachIndexed { index, value ->
                        PriorityButton(
                            priority = value,
                            label = labels[index],
                            labelStyle = labelStyle,
                            showLabel = showLabels,
                            selected = value == priority,
                            onClick = { onPriorityChange(value) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.PriorityButton(
    priority: Int,
    label: String,
    labelStyle: TextStyle,
    showLabel: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val flagColor = priorityColor(priority)
    val shape = RoundedCornerShape(12.dp)
    val labelColor =
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (selected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Column(
            modifier = Modifier.padding(
                vertical = 12.dp,
                horizontal = CARD_HORIZONTAL_PADDING,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (priority == Task.Priority.NONE) {
                    Icons.Outlined.OutlinedFlag
                } else {
                    Icons.Filled.Flag
                },
                contentDescription = if (showLabel) null else label,
                tint = flagColor,
                modifier = Modifier.size(24.dp),
            )
            if (showLabel) {
                Text(
                    text = label,
                    style = labelStyle,
                    color = labelColor,
                    maxLines = 1,
                )
            }
        }
    }
}
