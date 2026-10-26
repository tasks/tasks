package org.tasks.compose.tasklist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.tasks.compose.components.SymbolIcon
import org.tasks.themes.TasksIcons

internal const val DRAG_HANDLE_TAG = "task-row-drag"

/**
 * The per-row drag handle, shown only while manual ("My order") sorting is active.
 *
 * The gesture itself is not here: the caller applies the sh.calvin reorderable
 * `draggableHandle` modifier (created inside the [sh.calvin.reorderable.ReorderableItem]
 * scope) through [handleModifier]. This composable is the visual target and the
 * semantics node tests query; the library's press detector runs on the same node.
 */
@Composable
internal fun DragHandle(
    handleModifier: Modifier = Modifier,
) {
    // One flat node: no clickable child. The surrounding ListItem merges plain
    // descendants into the row node and drops their testTag; marking this node as
    // itself merging its descendants keeps it an independent, queryable semantics
    // node (the rule Compose uses for separately focusable elements).
    Box(
        modifier = handleModifier
            .size(48.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                this[SemanticsProperties.TestTag] = DRAG_HANDLE_TAG
                contentDescription = "Drag to reorder"
            },
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(
            name = TasksIcons.DRAG_INDICATOR,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
