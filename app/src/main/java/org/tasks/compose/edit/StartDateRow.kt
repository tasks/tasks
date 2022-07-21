package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.ui.StartDateControlSet.Companion.getRelativeDateString
import org.tasks.R
import org.tasks.compose.TaskEditRow
import org.tasks.dialogs.StartDatePicker

@Composable
fun StartDateRow(
    startDate: Long,
    selectedDay: Long,
    selectedTime: Int,
    currentTime: Long = DateUtilities.now(),
    hasDueDate: Boolean,
    printDate: () -> String,
    onClick: () -> Unit,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_pending_actions_24px,
        content = {
            StartDate(
                startDate = startDate,
                selectedDay = selectedDay,
                selectedTime = selectedTime,
                currentTime = currentTime,
                hasDueDate = hasDueDate,
                printDate = printDate,
            )
        },
        onClick = onClick
    )
}

@Composable
fun StartDate(
    startDate: Long,
    selectedDay: Long,
    selectedTime: Int,
    currentTime: Long,
    hasDueDate: Boolean,
    printDate: () -> String,
) {
    val context = LocalContext.current
    Text(
        text = when (selectedDay) {
            StartDatePicker.DUE_DATE -> context.getRelativeDateString(R.string.due_date, selectedTime)
            StartDatePicker.DUE_TIME -> context.getString(R.string.due_time)
            StartDatePicker.DAY_BEFORE_DUE -> context.getRelativeDateString(R.string.day_before_due, selectedTime)
            StartDatePicker.WEEK_BEFORE_DUE -> context.getRelativeDateString(R.string.week_before_due, selectedTime)
            in 1..Long.MAX_VALUE -> printDate()
            else -> stringResource(id = R.string.no_start_date)
        },
        color = when {
            selectedDay < 0 && !hasDueDate -> colorResource(id = R.color.overdue)
            startDate == 0L -> MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
            startDate < currentTime -> colorResource(id = R.color.overdue)
            else -> MaterialTheme.colors.onSurface
        },
        modifier = Modifier
            .padding(vertical = 20.dp)
            .height(24.dp),
    )
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoStartDate() {
    MdcTheme {
        StartDateRow(
            startDate = 0L,
            selectedDay = StartDatePicker.NO_DAY,
            selectedTime = StartDatePicker.NO_TIME,
            currentTime = 1657080392000L,
            hasDueDate = false,
            printDate = { "" },
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun FutureStartDate() {
    MdcTheme {
        StartDateRow(
            startDate = 1657080392000L,
            selectedDay = StartDatePicker.DUE_DATE,
            selectedTime = StartDatePicker.NO_TIME,
            currentTime = 1657080392000L,
            hasDueDate = false,
            printDate = { "" },
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun PastStartDate() {
    MdcTheme {
        StartDateRow(
            startDate = 1657080392000L,
            selectedDay = StartDatePicker.DUE_TIME,
            selectedTime = StartDatePicker.NO_TIME,
            currentTime = 1657080392001L,
            hasDueDate = false,
            printDate = { "" },
            onClick = {},
        )
    }
}