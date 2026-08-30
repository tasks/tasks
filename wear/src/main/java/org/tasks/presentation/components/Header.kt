/**
 * Header.kt — Section header for Wear OS scrollable lists.
 *
 * Renders a centered, horizontally-padded row that can optionally be tappable.
 * There are two overloads:
 * - `Header(text)` — Non-clickable; just renders a centered [Text].
 * - `Header(clickable, onClick, content)` — Optionally clickable; accepts
 *   arbitrary composable content (used by [CollapsibleHeader]).
 */
package org.tasks.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun Header(text: String) {
    Header(clickable = false) {
        Text(text)
    }
}

@Composable
internal fun Header(
    clickable: Boolean = true,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .fillMaxWidth()
            .clickable(enabled = clickable, onClick = onClick)
            .padding(12.dp)
    ) {
        content()
    }
}