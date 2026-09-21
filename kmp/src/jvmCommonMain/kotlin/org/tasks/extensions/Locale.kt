package org.tasks.extensions

import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale

fun Locale.formatNumber(number: Int, grouping: Boolean = true): String {
    return NumberFormat.getNumberInstance(this)
        .apply { isGroupingUsed = grouping }
        .format(number.toLong())
}

fun Locale.parseInteger(number: String?): Int? {
    return try {
        NumberFormat.getNumberInstance(this)
            .parse(number)
            .toLong()
            .takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }
            ?.toInt()
    } catch (e: ParseException) {
        null
    } catch (e: NullPointerException) {
        null
    }
}
