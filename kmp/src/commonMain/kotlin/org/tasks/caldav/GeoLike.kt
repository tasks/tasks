package org.tasks.caldav

import kotlin.math.min

private const val PLACE_ACCURACY = 4

fun Double.toLikeString(): String = toPlainString().toLikeString()

internal fun String.toLikeString(): String {
    val string = truncate()
    return when {
        !string.contains('.') -> "$string.0"
        string.numDecimalPlaces() < PLACE_ACCURACY -> string
        else -> "${string}%"
    }
}

private fun String.numDecimalPlaces(): Int {
    val index = indexOf(".")
    return if (index < 0) 0 else length - index - 1
}

private fun String.truncate(): String {
    val index = indexOf(".")
    return if (index < 0) this else substring(0, min(length, index + PLACE_ACCURACY + 1))
}

private fun Double.toPlainString(): String {
    if (this == 0.0) return "0"
    val string = toString()
    val e = string.indexOf('E')
    if (e < 0) return string.stripTrailingZeros()
    val negative = string.startsWith("-")
    val mantissa = string.substring(if (negative) 1 else 0, e)
    val exponent = string.substring(e + 1).toInt()
    val dot = mantissa.indexOf('.')
    val digits = mantissa.replace(".", "")
    val integerDigits = (if (dot < 0) digits.length else dot) + exponent
    val plain = when {
        integerDigits <= 0 -> "0." + "0".repeat(-integerDigits) + digits
        integerDigits >= digits.length -> digits + "0".repeat(integerDigits - digits.length)
        else -> digits.substring(0, integerDigits) + "." + digits.substring(integerDigits)
    }
    return (if (negative) "-" else "") + plain.stripTrailingZeros()
}

private fun String.stripTrailingZeros(): String =
    if (contains('.')) trimEnd('0').trimEnd('.') else this
