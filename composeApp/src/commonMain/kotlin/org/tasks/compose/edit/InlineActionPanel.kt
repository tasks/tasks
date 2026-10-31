package org.tasks.compose.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Corner radius for the inline action panels: the edit pane cards' radius
 * (16.dp, matching TaskEditCard in kmp) so the panel family shares the
 * pane's corner language. Kept in composeApp: the edit-card constant is
 * private to kmp and the panels are desktop-only, so this is the desktop
 * app's own token.
 */
val InlineActionPanelRadius = 16.dp

/**
 * Shared container for the inline action panels that push down the edit
 * content (delete preview, make-parent-task).
 *
 * Deliberately transparent with a hairline [MaterialTheme.colorScheme.outlineVariant]
 * border — the same line the unselected priority buttons use — rather than a
 * filled surface: a white card would visually associate the panel with the
 * edit cards (items) below, while sitting on the pane's grey background
 * associates it with the action bar above, which is what it is.
 *
 * The corner radius is [InlineActionPanelRadius], the edit cards' radius, so
 * the panel family shares the pane's corner language.
 */
@Composable
fun InlineActionPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(InlineActionPanelRadius),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}
