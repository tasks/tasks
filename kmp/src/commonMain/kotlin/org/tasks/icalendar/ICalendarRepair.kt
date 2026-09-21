package org.tasks.icalendar

private val INVALID_UTC_OFFSET = Regex(
    "^(TZOFFSET(FROM|TO):[+\\-]?)((18|19|[2-6]\\d)\\d\\d)$",
    setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE),
)

private val INVALID_DAY_OFFSET = Regex(
    "(?:^|^(?:DURATION|REFRESH-INTERVAL|RELATED-TO|TRIGGER);VALUE=)(?:DURATION|TRIGGER):(-?P((T-?\\d+D)|(-?\\d+DT)))$",
    setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE),
)

fun String.repairICalendar(): String = fixInvalidUtcOffsets().fixInvalidDayOffsets()

private fun String.fixInvalidUtcOffsets(): String =
    replace(INVALID_UTC_OFFSET) { "${it.groupValues[1]}00${it.groupValues[3]}" }

private fun String.fixInvalidDayOffsets(): String {
    var iCal = this
    for (match in INVALID_DAY_OFFSET.findAll(this).toList().reversed()) {
        val duration = match.groups[1] ?: continue
        iCal = iCal.replaceRange(duration.range, duration.value.replace("PT", "P").replace("DT", "D"))
    }
    return iCal
}
