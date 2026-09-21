package org.tasks.compose.pickers

const val NO_DAY = 0L
const val NO_TIME = 0
const val MULTIPLE_DAYS = -1L
const val MULTIPLE_TIMES = -1
const val DUE_DATE = -1L
const val DAY_BEFORE_DUE = -2L
const val WEEK_BEFORE_DUE = -3L
const val DUE_TIME = -4L

internal const val DEFAULT_MORNING = 9 * 60 * 60 * 1000
internal const val DEFAULT_AFTERNOON = 13 * 60 * 60 * 1000
internal const val DEFAULT_EVENING = 17 * 60 * 60 * 1000
internal const val DEFAULT_NIGHT = 20 * 60 * 60 * 1000

internal const val TIME_MARKER = 1000

data class QuickPickTimes(
    val morning: Int = DEFAULT_MORNING,
    val afternoon: Int = DEFAULT_AFTERNOON,
    val evening: Int = DEFAULT_EVENING,
    val night: Int = DEFAULT_NIGHT,
)
