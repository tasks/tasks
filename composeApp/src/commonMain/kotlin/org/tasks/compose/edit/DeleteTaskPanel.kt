package org.tasks.compose.edit

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.components.SymbolIcon
import org.tasks.themes.TasksIcons
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.delete_n_tasks
import tasks.kmp.generated.resources.delete_task
import tasks.kmp.generated.resources.delete_tree_protected
import tasks.kmp.generated.resources.delete_tree_title

/**
 * One row of the delete preview: what will be removed, indented by depth,
 * with its completed state and (for read-only tasks that the deletion skips)
 * a protected annotation.
 */
data class DeletePreviewRow(
    val title: String,
    val indent: Int = 0,
    val completed: Boolean = false,
    val readOnly: Boolean = false,
)

/**
 * The inline "these tasks will be deleted" panel: the full subtree about to
 * be removed (including completed and filter-hidden items) with a red Delete
 * button and a cross to cancel.
 *
 * The tree preview replaces the old yes/no confirmation dialog: for a tree of
 * dozens of tasks with no undo, "are you sure?" is not a confirmation, and
 * this panel is the one. Like the make-parent panel it expands inside the
 * edit pane rather than overlaying it, inside the shared [InlineActionPanel]
 * container so the two panels read as a family.
 */
@Composable
fun DeleteTaskPanel(
    modifier: Modifier = Modifier,
    rows: List<DeletePreviewRow>,
    working: Boolean,
    keyboardController: SoftwareKeyboardController? = null,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    InlineActionPanel(modifier = modifier) {
        Column(
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    when {
                        event.key == Key.Enter && !working -> {
                            keyboardController?.hide()
                            onDelete()
                            true
                        }
                        event.key == Key.Escape -> {
                            onCancel()
                            true
                        }
                        else -> false
                    }
                },
        ) {
            Text(
                text = stringResource(Res.string.delete_tree_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            Box(modifier = Modifier.heightIn(max = 200.dp)) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = (row.indent * 16 + 8).dp)
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SymbolIcon(
                                name = if (row.completed) TasksIcons.CHECK_CIRCLE
                                else TasksIcons.RADIO_BUTTON_UNCHECKED,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                contentDescription = null,
                            )
                            Text(
                                text = row.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (row.completed || row.readOnly)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                            if (row.readOnly) {
                                Text(
                                    text = stringResource(Res.string.delete_tree_protected),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            // Two filled buttons of comparable weight: the safe action (Cancel) on
            // the left, the danger action on the right. Cancel reuses the fold/unfold
            // subtask chip's surface (surfaceContainerHighest) so it reads as a neutral
            // control, and carries an X glyph alongside the label to reinforce X==cancel.
            // Delete follows the list-settings danger convention: pale pink fill, red
            // text, trash glyph, and a label that counts what will actually be removed.
            // Both use the theme's small shape (linked, not hardcoded) so the corners
            // track the rest of the app.
            val toDelete = rows.count { !it.readOnly }
            val deleteLabel = if (toDelete <= 1) {
                stringResource(Res.string.delete_task)
            } else {
                pluralStringResource(Res.plurals.delete_n_tasks, toDelete, toDelete)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onCancel()
                    },
                    enabled = !working,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    SymbolIcon(
                        name = TasksIcons.CLEAR,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(Res.string.cancel), maxLines = 1)
                }
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onDelete()
                    },
                    enabled = !working,
                    // Delete gets a fairer share of the row so "Delete N tasks" +
                    // the trash glyph stay on one line.
                    modifier = Modifier.weight(1.35f),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    SymbolIcon(
                        name = TasksIcons.DELETE,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(deleteLabel, maxLines = 1)
                }
            }
        }
    }
}
