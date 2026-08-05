package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import org.tasks.kmp.org.tasks.time.DateFormatter

@Composable
fun StartDateChip(
    sortGroup: Long?,
    startDate: Long,
    compact: Boolean,
    timeOnly: Boolean,
    colorProvider: (Int) -> Int,
    dateFormatter: DateFormatter,
) {
    org.tasks.compose.chips.StartDateChip(
        sortGroup = sortGroup,
        startDate = startDate,
        compact = compact,
        timeOnly = timeOnly,
        chipColor = remember { Color(colorProvider(0)) },
        dateFormatter = dateFormatter,
    )
}
