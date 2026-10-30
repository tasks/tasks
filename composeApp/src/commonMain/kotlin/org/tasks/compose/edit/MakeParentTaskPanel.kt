package org.tasks.compose.edit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.components.SymbolIcon
import org.tasks.themes.TasksIcons
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.make_parent_task
import tasks.kmp.generated.resources.make_parent_task_title

/**
 * The inline "move into new parent task" panel: a single row holding the new
 * parent's title plus accept (tick) and cancel (cross) buttons.
 *
 * Deliberately not a dialog: the point of the feature is to decide what
 * project this task belongs to *while looking at the task*, so the panel
 * expands inside the edit pane and nothing is overlaid.
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
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
        IconButton(
            onClick = {
                keyboardController?.hide()
                onAccept(title.trim())
            },
            enabled = canAccept,
        ) {
            SymbolIcon(
                name = TasksIcons.CHECK,
                contentDescription = stringResource(Res.string.make_parent_task),
            )
        }
        IconButton(onClick = onCancel, enabled = !working) {
            SymbolIcon(
                name = TasksIcons.CLEAR,
                contentDescription = stringResource(Res.string.cancel),
            )
        }
    }
}
