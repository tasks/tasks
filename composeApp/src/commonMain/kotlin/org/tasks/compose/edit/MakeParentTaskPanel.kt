package org.tasks.compose.edit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.components.SymbolIcon
import org.tasks.themes.TasksIcons
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.make_parent_task
import tasks.kmp.generated.resources.make_parent_task_title

/**
 * The inline "move into new parent task" panel: a single row holding the new
 * parent's title plus cancel (cross) and accept (floppy) buttons.
 *
 * Deliberately not a dialog: the point of the feature is to decide what
 * project this task belongs to *while looking at the task*, so the panel
 * expands inside the edit pane and nothing is overlaid.
 *
 * The row is [Cancel][Accept] like the delete panel's button row — the app
 * puts the safe action on the left and the commit on the right — and the
 * accept is a floppy, the app's save language, since accepting does save a
 * new task. The whole row sits in the shared [InlineActionPanel] container.
 */
@Composable
fun MakeParentTaskPanel(
    modifier: Modifier = Modifier,
    working: Boolean,
    keyboardController: SoftwareKeyboardController? = null,
    onAccept: (title: String) -> Unit,
    onCancel: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val canAccept = !working && title.isNotBlank()
    InlineActionPanel(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                // The field is an item of the edit pane: the pane's card
                // radius and the app's hairline (outlineVariant) line.
                shape = RoundedCornerShape(InlineActionPanelRadius),
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        when {
                            event.key == Key.Enter -> {
                                if (canAccept) {
                                    keyboardController?.hide()
                                    onAccept(title.trim())
                                }
                                true
                            }
                            event.key == Key.Escape -> {
                                onCancel()
                                true
                            }
                            else -> false
                        }
                    },
                placeholder = { Text(stringResource(Res.string.make_parent_task_title)) },
                singleLine = true,
                enabled = !working,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (canAccept) onAccept(title.trim())
                }),
            )
            IconButton(onClick = onCancel, enabled = !working) {
                SymbolIcon(
                    name = TasksIcons.CLEAR,
                    contentDescription = stringResource(Res.string.cancel),
                )
            }
            IconButton(
                onClick = {
                    keyboardController?.hide()
                    onAccept(title.trim())
                },
                enabled = canAccept,
            ) {
                SymbolIcon(
                    name = TasksIcons.SAVE,
                    contentDescription = stringResource(Res.string.make_parent_task),
                )
            }
        }
    }
}
