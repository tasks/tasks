package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.todoroo.andlib.utility.DateUtilities
import org.tasks.R
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.time.startOfDay
import java.time.format.FormatStyle

@Composable
fun StartDateChip(
    sortGroup: Long?,
    startDate: Long,
    compact: Boolean,
    timeOnly: Boolean,
    colorProvider: (Int) -> Int,
) {
    val context = LocalContext.current
    val text by remember(sortGroup, startDate, timeOnly, compact) {
        derivedStateOf {
            if (
                timeOnly &&
                sortGroup?.startOfDay() == startDate.startOfDay()
            ) {
                startDate
                    .takeIf { Task.hasDueTime(it) }
                    ?.let { DateUtilities.getTimeString(context, it.toDateTime()) }
            } else {
                DateUtilities.getRelativeDateTime(
                    context,
                    startDate,
                    context.resources.configuration.locales[0],
                    if (compact) FormatStyle.SHORT else FormatStyle.MEDIUM,
                    false,
                    false
                )
            }
        }
    }
    if (text != null) {
        Chip(
            icon = R.drawable.ic_pending_actions_24px,
            name = text,
            theme = 0,
            showText = true,
            showIcon = true,
            onClick = {},
            colorProvider = colorProvider,
        )
    }
}
