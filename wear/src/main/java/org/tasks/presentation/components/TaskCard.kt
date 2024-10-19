package org.tasks.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
                modifier = Modifier.alpha(if (hidden) .6f else 1f).weight(1f),
            )
            if (numSubtasks > 0) {
                Button(
                    onClick = toggleSubtasks,
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
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
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}
