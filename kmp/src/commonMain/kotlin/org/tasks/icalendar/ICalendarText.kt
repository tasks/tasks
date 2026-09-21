package org.tasks.icalendar

internal fun String.escapeICalText(): String =
    replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\\n").replace("\n", "\\n")

internal fun foldContentLine(line: String): String = buildString {
    var octets = 0
    for (char in line) {
        val width = when {
            char.code < 0x80 -> 1
            char.code < 0x800 -> 2
            char.isSurrogate() -> 2
            else -> 3
        }
        if (octets + width > 75) {
            append("\r\n ")
            octets = 1
        }
        append(char)
        octets += width
    }
    append("\r\n")
}

internal fun formatICalDuration(millis: Long): String = buildString {
    if (millis < 0) append('-')
    append('P')
    var seconds = kotlin.math.abs(millis) / 1_000
    val days = seconds / 86_400
    seconds %= 86_400
    if (days > 0) append(days).append('D')
    if (seconds > 0 || days == 0L) {
        append('T')
        val hours = seconds / 3_600
        val minutes = seconds % 3_600 / 60
        seconds %= 60
        if (hours > 0) append(hours).append('H')
        if (minutes > 0) append(minutes).append('M')
        if (seconds > 0 || (hours == 0L && minutes == 0L)) append(seconds).append('S')
    }
}
