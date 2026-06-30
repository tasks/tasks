/**
 * CollapsibleHeader.kt — Tappable section header with an animated chevron.
 *
 * Used in the task list to render group headers (e.g. "Overdue", "Today")
 * that the user can tap to collapse or expand. Delegates rendering to
 * [Header] and appends a [Chevron] after the title text.
 *
 * @param title     The section label (e.g. "Today").
 * @param collapsed `true` if the section is currently collapsed.
 * @param onClick   Callback to toggle the collapsed state.
 */
package org.tasks.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text

@Composable
fun CollapsibleHeader(
    title: String,
    collapsed: Boolean,
    onClick: () -> Unit,
) {
    Header(
        onClick = onClick,
    ) {
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Chevron(collapsed = collapsed)
    }
}
