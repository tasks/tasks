package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.data.Task
import org.tasks.compose.CheckBox
import org.tasks.compose.ClearButton
import org.tasks.compose.DisabledText
import org.tasks.compose.SubtaskChip
import org.tasks.compose.TaskEditIcon
import org.tasks.compose.TaskEditRow
import org.tasks.data.TaskContainer
import org.tasks.ui.TaskListViewModel

@Composable
fun SubtaskRow(
    originalFilter: Filter?,
    filter: Filter?,
    hasParent: Boolean,
    desaturate: Boolean,
    existingSubtasks: TaskListViewModel.TasksResults,
    newSubtasks: List<Task>,
    openSubtask: (Task) -> Unit,
    completeExistingSubtask: (Task, Boolean) -> Unit,
    completeNewSubtask: (Task) -> Unit,
    toggleSubtask: (Long, Boolean) -> Unit,
    addSubtask: () -> Unit,
    deleteSubtask: (Task) -> Unit,
) {
    TaskEditRow(
        icon = {
            TaskEditIcon(
                id = org.tasks.R.drawable.ic_subdirectory_arrow_right_black_24dp,
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        top = 20.dp,
                        end = 20.dp,
                        bottom = 20.dp
                    )
                    .alpha(ContentAlpha.medium),
            )
        },
        content = {
            Column {
                val isGoogleTaskChild =
                    hasParent &&
                            filter is GtasksFilter &&
                            originalFilter is GtasksFilter &&
                            originalFilter.remoteId == filter.remoteId
                if (isGoogleTaskChild) {
                    DisabledText(
                        text = stringResource(id = org.tasks.R.string.subtasks_multilevel_google_task),
                        modifier = Modifier.padding(top = 20.dp, bottom = 20.dp, end = 16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(height = 8.dp))
                    if (existingSubtasks is TaskListViewModel.TasksResults.Results) {
                        existingSubtasks.tasks.forEach { task ->
                            ExistingSubtaskRow(
                                task = task,
                                desaturate = desaturate,
                                indent = if (filter !is GtasksFilter) task.indent else 0,
                                onRowClick = { openSubtask(task.task) },
                                onCompleteClick = {
                                    completeExistingSubtask(
                                        task.task,
                                        !task.isCompleted
                                    )
                                },
                                onToggleSubtaskClick = { toggleSubtask(task.id, !task.isCollapsed) }
                            )
                        }
                    }
                    newSubtasks.forEach { subtask ->
                        NewSubtaskRow(
                            subtask = subtask,
                            desaturate = desaturate,
                            addSubtask = addSubtask,
                            onComplete = completeNewSubtask,
                            onDelete = deleteSubtask,
                        )
                    }
                    DisabledText(
                        text = stringResource(id = org.tasks.R.string.TEA_add_subtask),
                        modifier = Modifier
                            .clickable { addSubtask() }
                            .padding(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
    )
}

@Composable
fun NewSubtaskRow(
    subtask: Task,
    desaturate: Boolean,
    addSubtask: () -> Unit,
    onComplete: (Task) -> Unit,
    onDelete: (Task) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CheckBox(
            task = subtask,
            onCompleteClick = { onComplete(subtask) },
            modifier = Modifier.align(Alignment.Top),
            desaturate = desaturate,
        )
        var text by remember { mutableStateOf(subtask.title ?: "") }
        val focusRequester = remember { FocusRequester() }
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                subtask.title = it
            },
            cursorBrush = SolidColor(MaterialTheme.colors.onSurface),
            modifier = Modifier
                .weight(1f)
                .focusable(enabled = true)
                .focusRequester(focusRequester)
                .alpha(if (subtask.isCompleted) ContentAlpha.disabled else ContentAlpha.high)
                .align(Alignment.Top)
                .padding(top = 12.dp),
            textStyle = MaterialTheme.typography.body1.copy(
                textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = MaterialTheme.colors.onSurface,
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (text.isNotBlank()) {
                        addSubtask()
                    }
                }
            ),
            singleLine = false,
            maxLines = Int.MAX_VALUE,
        )
        ClearButton { onDelete(subtask) }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun ExistingSubtaskRow(
    task: TaskContainer, indent: Int,
    desaturate: Boolean,
    onRowClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onToggleSubtaskClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onRowClick() }
            .padding(end = 16.dp)
    ) {
        Spacer(modifier = Modifier.width((indent * 20).dp))
        CheckBox(
            task = task.task,
            onCompleteClick = onCompleteClick,
            desaturate = desaturate,
            modifier = Modifier.align(Alignment.Top),
        )
        Text(
            text = task.title!!,
            modifier = Modifier
                .weight(1f)
                .alpha(if (task.isCompleted || task.isHidden) ContentAlpha.disabled else ContentAlpha.high)
                .align(Alignment.Top)
                .padding(top = 12.dp),
            style = MaterialTheme.typography.body1.copy(
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
        )
        if (task.hasChildren()) {
            SubtaskChip(
                collapsed = task.isCollapsed,
                children = task.children,
                compact = true,
                onClick = onToggleSubtaskClick,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoSubtasks() {
    MdcTheme {
        SubtaskRow(
            originalFilter = null,
            filter = null,
            hasParent = false,
            desaturate = true,
            existingSubtasks = TaskListViewModel.TasksResults.Results(emptyList()),
            newSubtasks = emptyList(),
            openSubtask = {},
            completeExistingSubtask = { _, _ -> },
            completeNewSubtask = {},
            toggleSubtask = { _, _ -> },
            addSubtask = {},
            deleteSubtask = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun SubtasksPreview() {
    MdcTheme {
        SubtaskRow(
            originalFilter = null,
            filter = null,
            hasParent = false,
            desaturate = true,
            existingSubtasks = TaskListViewModel.TasksResults.Results(
                listOf(
                    TaskContainer(
                        task = Task(
                            title = "Existing subtask 1",
                            priority = Task.Priority.HIGH,
                        ),
                        indent = 0
                    ),
                    TaskContainer(
                        task = Task(
                            title = "Existing subtask 2 with a really long title",
                            priority = Task.Priority.LOW,
                        ),
                        indent = 1
                    )
                )
            ),
            newSubtasks = listOf(
                Task().apply {
                    title = "New subtask 1"
                },
                Task().apply {
                    title = "New subtask 2 with a really long title"
                },
                Task(),
            ),
            openSubtask = {},
            completeExistingSubtask = { _, _ -> },
            completeNewSubtask = {},
            toggleSubtask = { _, _ -> },
            addSubtask = {},
            deleteSubtask = {},
        )
    }
}