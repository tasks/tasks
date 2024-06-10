package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditRow
import org.tasks.themes.TasksTheme

@Composable
fun DueDateRow(
    dueDate: String?,
    overdue: Boolean,
    onClick: () -> Unit,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_outline_schedule_24px,
        content = {
            DueDate(
                dueDate = dueDate,
                overdue = overdue,
            )
        },
        onClick = onClick,
    )
}

@Composable
fun DueDate(dueDate: String?, overdue: Boolean) {
    if (dueDate.isNullOrBlank()) {
        DisabledText(
            text = stringResource(id = R.string.no_due_date),
            modifier = Modifier.padding(top = 20.dp, bottom = 20.dp, end = 16.dp)
        )
    } else {
        Text(
            text = dueDate,
            color = if (overdue) {
                colorResource(id = R.color.overdue)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(top = 20.dp, bottom = 20.dp, end = 16.dp)
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun DueDatePreview() {
    TasksTheme {
        DueDateRow(
            dueDate = "Today",
            overdue = false,
        ) {}
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoDueDatePreview() {
    TasksTheme {
        DueDateRow(
            dueDate = null,
            overdue = false,
        ) {}
    }
}