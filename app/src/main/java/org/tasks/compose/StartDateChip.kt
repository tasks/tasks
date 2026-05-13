package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.tasks.extensions.Context.is24HourFormat

@Composable
fun StartDateChip(
    sortGroup: Long?,
    startDate: Long,
    compact: Boolean,
    timeOnly: Boolean,
    colorProvider: (Int) -> Int,
) {
    val is24Hour = LocalContext.current.is24HourFormat
    org.tasks.compose.chips.StartDateChip(
        sortGroup = sortGroup,
        startDate = startDate,
        compact = compact,
        timeOnly = timeOnly,
        is24HourFormat = is24Hour,
        chipColor = remember { Color(colorProvider(0)) },
    )
}
