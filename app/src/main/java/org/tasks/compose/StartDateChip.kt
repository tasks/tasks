package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.runBlocking
import org.tasks.data.entity.Task
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.kmp.org.tasks.time.getTimeString
import org.tasks.themes.TasksIcons
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay

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
                    ?.let { getTimeString(currentTimeMillis(), context.is24HourFormat) }
            } else {
                runBlocking {
                    getRelativeDateTime(
                        startDate,
                        context.is24HourFormat,
                        if (compact) DateStyle.SHORT else DateStyle.MEDIUM
                    )
                }
            }
        }
    }
    if (text != null) {
        Chip(
            icon = TasksIcons.PENDING_ACTIONS,
            name = text,
            theme = 0,
            showText = true,
            showIcon = true,
            onClick = {},
            colorProvider = colorProvider,
        )
    }
}
