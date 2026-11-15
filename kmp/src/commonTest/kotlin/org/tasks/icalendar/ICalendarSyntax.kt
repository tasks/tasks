package org.tasks.icalendar

private val CONTENT_LINE = Regex("^[A-Za-z0-9-]+(;[^:]*)?:")

private val FOLD = Regex("\r?\n[ \t]")

private val LINE_BREAK = Regex("\r?\n")

fun String.isWellFormedICalendar(): Boolean {
    val lines = replace(FOLD, "").split(LINE_BREAK).dropLastWhile { it.isEmpty() }
    return lines.isNotEmpty() && lines.all { CONTENT_LINE.containsMatchIn(it) }
}
