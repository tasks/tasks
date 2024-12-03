package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditRow
import org.tasks.data.entity.Task
import org.tasks.themes.TasksTheme

@Composable
fun RepeatRow(
    recurrence: String?,
    @Task.RepeatFrom repeatFrom: Int,
    onClick: () -> Unit,
    onRepeatFromChanged: (@Task.RepeatFrom Int) -> Unit,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_outline_repeat_24px,
        content = {
            Repeat(
                recurrence = recurrence,
                repeatFrom = repeatFrom,
                onRepeatFromChanged = onRepeatFromChanged,
            )
        },
        onClick = onClick,
    )
}

@Composable
fun Repeat(
    recurrence: String?,
    repeatFrom: @Task.RepeatFrom Int,
    onRepeatFromChanged: (@Task.RepeatFrom Int) -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(20.dp))
        if (recurrence.isNullOrBlank()) {
            DisabledText(text = stringResource(id = R.string.repeat_option_does_not_repeat))
        } else {
            Text(
                text = recurrence,
                modifier = Modifier.defaultMinSize(minHeight = 24.dp).padding(end = 16.dp).fillMaxWidth(),
                maxLines = Int.MAX_VALUE,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text(
                    text = stringResource(id = R.string.repeats_from),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(4.dp))
                var expanded by remember { mutableStateOf(false) }
                Text(
                    text = stringResource(
                        id = when (repeatFrom) {
                            Task.RepeatFrom.COMPLETION_DATE -> R.string.repeat_type_completion
                            else -> R.string.repeat_type_due
                        }
                    ),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = TextDecoration.Underline,
                    ),
                    modifier = Modifier.clickable { expanded = true },
                    color = MaterialTheme.colorScheme.onSurface,
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onRepeatFromChanged(Task.RepeatFrom.DUE_DATE)
                        },
                        text = {
                            Text(
                                text = stringResource(id = R.string.repeat_type_due),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    )
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onRepeatFromChanged(Task.RepeatFrom.COMPLETION_DATE)
                        },
                        text = {
                            Text(
                                text = stringResource(id = R.string.repeat_type_completion),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun RepeatPreview() {
    TasksTheme {
        RepeatRow(
            recurrence = "Repeats weekly on Mon, Tue, Wed, Thu, Fri",
            repeatFrom = Task.RepeatFrom.DUE_DATE,
            onClick = {},
            onRepeatFromChanged = {},
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoRepeatPreview() {
    TasksTheme {
        RepeatRow(
            recurrence = null,
            repeatFrom = Task.RepeatFrom.DUE_DATE,
            onClick = {},
            onRepeatFromChanged = {},
        )
    }
}