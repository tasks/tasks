package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.plusDays
import org.tasks.time.startOfDay

private const val TODAY_REFRESH_INTERVAL_MS = 10 * 60 * 1000L

private val todayFlow: StateFlow<Long> =
    flow {
        while (true) {
            val now = currentTimeMillis()
            emit(now.startOfDay())
            val untilMidnight = (now.startOfDay().plusDays(1) - now).coerceAtLeast(1L)
            delay(untilMidnight.coerceAtMost(TODAY_REFRESH_INTERVAL_MS))
        }
    }
        .distinctUntilChanged()
        .stateIn(
            scope = CoroutineScope(Dispatchers.Default),
            started = SharingStarted.WhileSubscribed(),
            initialValue = currentTimeMillis().startOfDay(),
        )

@Composable
fun rememberRelativeDateTime(
    timestamp: Long,
    is24Hour: Boolean,
    style: DateStyle = DateStyle.FULL,
    alwaysDisplayFullDate: Boolean = false,
): String {
    val today by todayFlow.collectAsState()
    return remember(timestamp, is24Hour, style, alwaysDisplayFullDate, today) {
        runBlocking {
            getRelativeDateTime(timestamp, is24Hour, style, alwaysDisplayFullDate = alwaysDisplayFullDate)
        }
    }
}
