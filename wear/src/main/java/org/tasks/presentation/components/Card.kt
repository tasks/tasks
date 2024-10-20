package org.tasks.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.MaterialTheme

@Composable
fun Card(
    backgroundColor: Color = MaterialTheme.colors.surface,
    icon: @Composable () -> Unit = {},
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
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
            content()
        }
    }
}
