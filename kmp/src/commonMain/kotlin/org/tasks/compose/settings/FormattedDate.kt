package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import org.tasks.kmp.org.tasks.time.DateFormatter
import org.tasks.kmp.org.tasks.time.DateStyle

@Composable
internal fun formattedDate(timestamp: Long, style: DateStyle = DateStyle.MEDIUM): String? {
    val formatted by produceState<String?>(initialValue = null, key1 = timestamp, key2 = style) {
        value = DateFormatter.create(is24HourFormat = false).fullDate(timestamp, style)
    }
    return formatted
}
