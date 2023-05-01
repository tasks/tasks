package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditRow

@Composable
fun RepeatRow(
    recurrence: String?,
    repeatAfterCompletion: Boolean,
    onClick: () -> Unit,
    onRepeatFromChanged: (Boolean) -> Unit,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_outline_repeat_24px,
        content = {
            Repeat(
                recurrence = recurrence,
                repeatFromCompletion = repeatAfterCompletion,
                onRepeatFromChanged = onRepeatFromChanged,
            )
        },
        onClick = onClick,
    )
}

@Composable
fun Repeat(
    recurrence: String?,
    repeatFromCompletion: Boolean,
    onRepeatFromChanged: (Boolean) -> Unit,
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
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text(text = stringResource(id = R.string.repeats_from))
                Spacer(modifier = Modifier.width(4.dp))
                var expanded by remember { mutableStateOf(false) }
                Text(
                    text = stringResource(
                        id = if (repeatFromCompletion)
                            R.string.repeat_type_completion
                        else
                            R.string.repeat_type_due
                    ),
                    style = MaterialTheme.typography.body1.copy(
                        textDecoration = TextDecoration.Underline,
                    ),
                    modifier = Modifier.clickable { expanded = true }
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onRepeatFromChanged(false)
                        }
                    ) {
                        Text(text = stringResource(id = R.string.repeat_type_due))
                    }
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onRepeatFromChanged(true)
                        }
                    ) {
                        Text(text = stringResource(id = R.string.repeat_type_completion))
                    }
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
    MdcTheme {
        RepeatRow(
            recurrence = "Repeats weekly on Mon, Tue, Wed, Thu, Fri",
            repeatAfterCompletion = false,
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
    MdcTheme {
        RepeatRow(
            recurrence = null,
            repeatAfterCompletion = false,
            onClick = {},
            onRepeatFromChanged = {},
        )
    }
}