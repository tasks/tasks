package org.tasks.extensions

import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale

fun Locale.formatNumber(number: Int): String {
    return NumberFormat.getNumberInstance(this).format(number.toLong())
}

fun Locale.parseInteger(number: String?): Int? {
    return try {
        NumberFormat.getNumberInstance(this).parse(number).toInt()
    } catch (e: ParseException) {
        null
    }
}
