package org.tasks.compose.chips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import org.tasks.data.entity.Task
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.DateFormatter
import org.tasks.kmp.formatTime
import org.tasks.themes.TasksIcons
import org.tasks.time.startOfDay

@Composable
fun StartDateChip(
    sortGroup: Long?,
    startDate: Long,
    compact: Boolean,
    timeOnly: Boolean,
    chipColor: Color,
    dateFormatter: DateFormatter?,
) {
    val text by remember(sortGroup, startDate, timeOnly, compact, dateFormatter) {
        derivedStateOf {
            if (
                timeOnly &&
                sortGroup?.startOfDay() == startDate.startOfDay()
            ) {
                startDate
                    .takeIf { Task.hasDueTime(it) }
                    ?.let { dateFormatter?.time(it) }
            } else {
                dateFormatter?.relativeDateTime(
                    startDate,
                    if (compact) DateStyle.SHORT else DateStyle.MEDIUM,
                )
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
