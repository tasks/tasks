/**
 * Card.kt — Reusable card wrapper for Wear OS list items.
 *
 * Wraps the standard Wear [Card] composable with a simpler API
 * that provides:
 * - A flat [backgroundColor] (no gradient) via [ColorPainter].
 * - Zero content padding so callers can control their own spacing.
 * - An optional leading [icon] slot (e.g. a checkbox) aligned vertically
 *   with the main [content] in a [Row].
 *
 * Used by [TaskCard], [EmptyCard], and anywhere a clickable card is needed
 * in the task list.
 */
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

/**
 * A simplified card composable for Wear OS.
 *
 * @param backgroundColor Background color of the card (defaults to the theme surface color).
 * @param icon            Optional leading composable (e.g. a [Checkbox]).
 * @param onClick         Callback when the card is tapped.
 * @param content         The main body composable rendered in a [RowScope].
 */
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
