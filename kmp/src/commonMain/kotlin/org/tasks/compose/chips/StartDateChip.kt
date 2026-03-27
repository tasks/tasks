package org.tasks.compose.chips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.runBlocking
import org.tasks.data.entity.Task
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.kmp.org.tasks.time.getTimeString
import org.tasks.themes.TasksIcons
import org.tasks.time.startOfDay

@Composable
fun StartDateChip(
    sortGroup: Long?,
    startDate: Long,
    compact: Boolean,
    timeOnly: Boolean,
    is24HourFormat: Boolean,
    chipColor: Color,
) {
    val text by remember(sortGroup, startDate, timeOnly, compact) {
        derivedStateOf {
            if (
                timeOnly &&
                sortGroup?.startOfDay() == startDate.startOfDay()
            ) {
                startDate
                    .takeIf { Task.hasDueTime(it) }
                    ?.let { getTimeString(it, is24HourFormat) }
            } else {
                runBlocking {
                    getRelativeDateTime(
                        startDate,
                        is24HourFormat,
                        if (compact) DateStyle.SHORT else DateStyle.MEDIUM,
                    )
                }
            }
        }
    }
    if (text != null) {
        Chip(
            icon = TasksIcons.PENDING_ACTIONS,
            text = text,
            color = chipColor,
        )
    }
}
