package org.tasks.compose.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.tasks.compose.components.TasksIcon
import org.tasks.compose.settings.CardPosition

val TaskEditCardHorizontalPadding = 20.dp

private val RowVerticalPadding = 16.dp
private val IconChipSize = 40.dp
private val IconChipRadius = 10.dp
private val IconTextGap = 16.dp
private const val IconChipAlpha = 0.12f

val TaskEditCardContentStartPadding = TaskEditCardHorizontalPadding + IconChipSize + IconTextGap

@Composable
fun TaskEditCardRow(
    value: String,
    valueColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable (ColumnScope.() -> Unit)? = null,
) {
    if (content == null) {
        TaskEditCard(modifier = modifier, onClick = onClick) {
            TaskEditCardRowContent(
                value = value,
                valueColor = valueColor,
                title = title,
                icon = icon,
                iconTint = iconTint,
            )
        }
        return
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TaskEditCardGap),
    ) {
        TaskEditCard(onClick = onClick, position = CardPosition.First) {
            TaskEditCardRowContent(
                value = value,
                valueColor = valueColor,
                title = title,
                icon = icon,
                iconTint = iconTint,
            )
        }
        TaskEditCard(position = CardPosition.Last) {
            Column(
                modifier = Modifier.padding(
                    start = TaskEditCardContentStartPadding,
                    end = TaskEditCardHorizontalPadding,
                    top = RowVerticalPadding,
                    bottom = RowVerticalPadding,
                ),
                content = content,
            )
        }
    }
}

@Composable
fun TaskEditCardRowContent(
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier.padding(
            horizontal = TaskEditCardHorizontalPadding,
            vertical = RowVerticalPadding,
        ),
    ) {
        if (icon != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(IconChipSize)
                    .background(
                        color = iconTint.copy(alpha = IconChipAlpha),
                        shape = RoundedCornerShape(IconChipRadius),
                    ),
            ) {
                TasksIcon(label = icon, tint = iconTint)
            }
        } else {
            Spacer(modifier = Modifier.size(IconChipSize))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = IconTextGap),
        ) {
            if (title != null) {
                TaskEditSectionLabel(text = title)
            }
            val base = MaterialTheme.typography.bodyLarge
            val valueStyle = remember(base, valueColor) {
                base.copy(color = valueColor, fontWeight = FontWeight.Medium)
            }
            Text(text = value, style = valueStyle)
        }
    }
}

