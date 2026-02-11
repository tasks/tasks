package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
            Row {
                Text(
                    text = stringResource(id = R.string.repeats_from),
                    modifier = Modifier.align(CenterVertically),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(4.dp))
                OutlinedButton(
                    onClick = {
                        onRepeatFromChanged(
                            if (repeatFrom == Task.RepeatFrom.DUE_DATE)
                                Task.RepeatFrom.COMPLETION_DATE
                            else
                                Task.RepeatFrom.DUE_DATE
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = OutlinedTextFieldDefaults.shape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.align(CenterVertically),
                ) {
                    Text(
                        text = stringResource(
                            id = when (repeatFrom) {
                                Task.RepeatFrom.COMPLETION_DATE -> R.string.repeat_type_completion
                                else -> R.string.repeat_type_due
                            }
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
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
