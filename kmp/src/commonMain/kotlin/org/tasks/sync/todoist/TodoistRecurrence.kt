package org.tasks.sync.todoist

import org.tasks.data.entity.Task
import org.tasks.repeats.ByDay
import org.tasks.repeats.Frequency
import org.tasks.repeats.Recur
import org.tasks.repeats.Weekday

/**
 * Translates between Tasks' RRULE-based [Recur] and Todoist's natural-language
 * recurrence strings ("every 2 weeks on mon").
 *
 * Todoist has no structured recurrence field at all -- the `due.string` a user typed
 * *is* the rule, and the server re-parses it. That makes this mapping inherently
 * partial, so both directions are conservative: anything not confidently recognised
 * returns null and the caller preserves the original value untouched rather than
 * writing a lossy approximation. Never widen this to a best-effort guess; silently
 * changing someone's repeat schedule is worse than not syncing it.
 *
 * The `every!` prefix means "repeat relative to the completion date", which is exactly
 * Tasks' [Task.RepeatFrom.COMPLETION_DATE].
 */
object TodoistRecurrence {
    private const val COMPLETION_PREFIX = "every!"
    private const val DUE_PREFIX = "every"

    private val WEEKDAYS = mapOf(
        "mon" to Weekday.MO, "monday" to Weekday.MO,
        "tue" to Weekday.TU, "tues" to Weekday.TU, "tuesday" to Weekday.TU,
        "wed" to Weekday.WE, "weds" to Weekday.WE, "wednesday" to Weekday.WE,
        "thu" to Weekday.TH, "thur" to Weekday.TH, "thurs" to Weekday.TH, "thursday" to Weekday.TH,
        "fri" to Weekday.FR, "friday" to Weekday.FR,
        "sat" to Weekday.SA, "saturday" to Weekday.SA,
        "sun" to Weekday.SU, "sunday" to Weekday.SU,
    )

    private val WEEKDAY_NAMES = mapOf(
        Weekday.MO to "mon",
        Weekday.TU to "tue",
        Weekday.WE to "wed",
        Weekday.TH to "thu",
        Weekday.FR to "fri",
        Weekday.SA to "sat",
        Weekday.SU to "sun",
    )

    private val WORKDAYS = listOf(Weekday.MO, Weekday.TU, Weekday.WE, Weekday.TH, Weekday.FR)

    /** Matches a leading interval, e.g. the "3" in "every 3 weeks". */
    private val INTERVAL = Regex("""^(\d+)\s+""")

    /** Matches an ordinal day of month, e.g. "15th", "3rd", "1st". */
    private val ORDINAL = Regex("""^(\d{1,2})(?:st|nd|rd|th)$""")

    data class Parsed(val recur: Recur, val repeatFrom: Int)

    /**
     * @return the parsed rule, or null when [string] isn't a form we recognise -- in
     *   which case the caller must leave the existing recurrence alone.
     */
    fun fromString(string: String?): Parsed? {
        val normalized = string?.trim()?.lowercase() ?: return null
        if (normalized.isEmpty()) {
            return null
        }
        val repeatFrom = when {
            normalized.startsWith(COMPLETION_PREFIX) -> Task.RepeatFrom.COMPLETION_DATE
            normalized.startsWith(DUE_PREFIX) -> Task.RepeatFrom.DUE_DATE
            else -> return null
        }
        var rest = normalized
            .removePrefix(COMPLETION_PREFIX)
            .let { if (it == normalized) it.removePrefix(DUE_PREFIX) else it }
            .trim()
        if (rest.isEmpty()) {
            return null
        }

        // "every other week" is Todoist shorthand for an interval of 2.
        var interval = 1
        if (rest.startsWith("other ")) {
            interval = 2
            rest = rest.removePrefix("other ").trim()
        } else {
            INTERVAL.find(rest)?.let { match ->
                val parsed = match.groupValues[1].toIntOrNull()
                if (parsed == null || parsed < 1) {
                    return null
                }
                interval = parsed
                rest = rest.removeRange(match.range).trim()
            }
        }

        val recur = parseUnit(rest, interval) ?: return null
        return Parsed(recur, repeatFrom)
    }

    private fun parseUnit(rest: String, interval: Int): Recur? {
        val intervalOrNull = interval.takeIf { it > 1 }

        // Time-of-day aliases all mean "once a day".
        if (rest in setOf("day", "days", "morning", "evening", "afternoon", "night")) {
            return Recur(frequency = Frequency.DAILY, interval = intervalOrNull)
        }
        if (rest in setOf("workday", "workdays", "weekday", "weekdays", "work day", "work days")) {
            return Recur(
                frequency = Frequency.WEEKLY,
                interval = intervalOrNull,
                byDay = WORKDAYS.map { ByDay(it) },
            )
        }
        if (rest in setOf("week", "weeks")) {
            return Recur(frequency = Frequency.WEEKLY, interval = intervalOrNull)
        }
        if (rest in setOf("month", "months")) {
            return Recur(frequency = Frequency.MONTHLY, interval = intervalOrNull)
        }
        if (rest in setOf("year", "years")) {
            return Recur(frequency = Frequency.YEARLY, interval = intervalOrNull)
        }
        if (rest == "last day") {
            return Recur(
                frequency = Frequency.MONTHLY,
                interval = intervalOrNull,
                byMonthDay = listOf(-1),
            )
        }

        // "every 2 weeks on mon, wed" -- the unit is explicit.
        val onSplit = rest.split(" on ", limit = 2)
        if (onSplit.size == 2) {
            val unit = onSplit[0].trim()
            val days = parseWeekdays(onSplit[1]) ?: return null
            return when (unit) {
                "week", "weeks" -> Recur(
                    frequency = Frequency.WEEKLY,
                    interval = intervalOrNull,
                    byDay = days.map { ByDay(it) },
                )
                else -> null
            }
        }

        // "every mon, wed" -- weekly with the unit implied.
        parseWeekdays(rest)?.let { days ->
            return Recur(
                frequency = Frequency.WEEKLY,
                interval = intervalOrNull,
                byDay = days.map { ByDay(it) },
            )
        }

        // "every 15th" -- monthly on a day of the month.
        ORDINAL.find(rest)?.let { match ->
            val day = match.groupValues[1].toIntOrNull()?.takeIf { it in 1..31 } ?: return null
            return Recur(
                frequency = Frequency.MONTHLY,
                interval = intervalOrNull,
                byMonthDay = listOf(day),
            )
        }

        return null
    }

    /** @return the listed weekdays, or null if any token isn't a weekday. */
    private fun parseWeekdays(input: String): List<Weekday>? {
        val tokens = input
            .split(",", " and ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) {
            return null
        }
        val days = tokens.map { WEEKDAYS[it] ?: return null }
        return days.distinct()
    }

    /**
     * @return a Todoist-parseable string, or null when [recur] uses features Todoist
     *   can't express -- in which case the caller must not send a recurrence at all.
     */
    fun toString(recur: Recur, repeatFrom: Int): String? {
        // Todoist has no equivalent of an end condition on a repeat, and no
        // positional ("2nd tuesday") or sub-daily recurrence.
        if (recur.count != null || recur.until != null) {
            return null
        }
        if (recur.byDay.any { it.offset != 0 }) {
            return null
        }
        if (recur.bySetPos.isNotEmpty() ||
            recur.byWeekNo.isNotEmpty() ||
            recur.byYearDay.isNotEmpty() ||
            recur.byHour.isNotEmpty() ||
            recur.byMinute.isNotEmpty() ||
            recur.bySecond.isNotEmpty()
        ) {
            return null
        }
        val interval = (recur.interval ?: 1).coerceAtLeast(1)
        val prefix = if (repeatFrom == Task.RepeatFrom.COMPLETION_DATE) "every!" else "every"

        fun withInterval(singular: String, plural: String): String =
            if (interval == 1) "$prefix $singular" else "$prefix $interval $plural"

        return when (recur.frequency) {
            Frequency.DAILY -> withInterval("day", "days")
            Frequency.WEEKLY -> {
                val days = recur.byDay.map { it.day }
                if (days.isEmpty()) {
                    withInterval("week", "weeks")
                } else {
                    val names = days.mapNotNull { WEEKDAY_NAMES[it] }
                    if (names.size != days.size) {
                        return null
                    }
                    val joined = names.joinToString(", ")
                    if (interval == 1) "$prefix $joined" else "$prefix $interval weeks on $joined"
                }
            }
            Frequency.MONTHLY -> when {
                recur.byMonthDay.isEmpty() -> withInterval("month", "months")
                recur.byMonthDay == listOf(-1) ->
                    if (interval == 1) "$prefix last day" else null
                recur.byMonthDay.size == 1 && recur.byMonthDay.single() in 1..31 ->
                    if (interval == 1) {
                        "$prefix ${ordinal(recur.byMonthDay.single())}"
                    } else {
                        null
                    }
                else -> null
            }
            Frequency.YEARLY ->
                if (recur.byMonth.isEmpty() && recur.byMonthDay.isEmpty()) {
                    withInterval("year", "years")
                } else {
                    // Todoist infers the month and day from the due date, so an
                    // explicit BYMONTH/BYMONTHDAY would be dropped.
                    null
                }
            Frequency.HOURLY,
            Frequency.MINUTELY,
            Frequency.SECONDLY -> null
        }
    }

    private fun ordinal(day: Int): String {
        val suffix = when {
            day % 100 in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$day$suffix"
    }
}
