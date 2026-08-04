/**
 * TaskCard.kt — Visual card for a single task in the Wear OS task list.
 *
 * Layout:
 * ```
 * ┌──────────────────────────────────────────────┐
 * │ [icon]  Task title text       Timestamp / ▼  │
 * │                              (subtask count)  │
 * └──────────────────────────────────────────────┘
 * ```
 *
 * Features:
 * - Leading [icon] slot (typically a [Checkbox] from [TaskRow]).
 * - Two-line ellipsised title with optional 60 % opacity for hidden tasks.
 * - If the task has subtasks, a clickable button with a [Chevron] and subtask
 *   count replaces the plain timestamp.
 * - Otherwise a small [Timestamp] label is rendered on the right.
 *
 * @see TaskRow   — Convenience wrapper that plugs a [Checkbox] into the icon slot.
 * @see Chevron   — Animated expand/collapse arrow.
 * @see Timestamp — Tiny caption showing the task's due date/time.
 */
package org.tasks.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun TaskCard(
    text: String,
    timestamp: String?,
    hidden: Boolean = false,
    numSubtasks: Int = 0,
    subtasksCollapsed: Boolean = false,
    toggleSubtasks: () -> Unit = {},
    icon: @Composable () -> Unit = {},
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = MaterialTheme.colors.onSurface,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        backgroundPainter = ColorPainter(backgroundColor),
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Text(
                text = text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = contentColor,
                modifier = Modifier
                    .alpha(if (hidden) .6f else 1f)
                    .weight(1f),
            )
            if (numSubtasks > 0) {
                Button(
                    onClick = toggleSubtasks,
                    colors = ButtonDefaults.outlinedButtonColors(),
                    shape = if (timestamp.isNullOrBlank()) CircleShape else RoundedCornerShape(16.dp),
                    modifier = Modifier.wrapContentWidth(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                    ) {
                        Timestamp(timestamp, contentColor)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = numSubtasks.toString(), // TODO: use number formatter
                                color = contentColor,
                            )
                            Chevron(subtasksCollapsed)
                        }
                    }
                }
            } else {
                Timestamp(timestamp, contentColor)
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}

@Composable
private fun Timestamp(
    timestamp: String?,
    color: Color,
) {
    if (timestamp.isNullOrBlank()) {
        return
    }
    Text(
        text = timestamp,
        color = color,
        style = MaterialTheme.typography.caption2,
        modifier = Modifier
            .alpha(.6f)
            .padding(start = 4.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
