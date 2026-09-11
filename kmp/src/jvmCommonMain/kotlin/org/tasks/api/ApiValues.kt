package org.tasks.api

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ApiValues(private val values: Map<String, Any?>) {

    val keys: Set<String> get() = values.keys

    fun containsKey(key: String) = values.containsKey(key)

    fun get(key: String): Any? = values[key]

    companion object {
        fun of(vararg pairs: Pair<String, Any?>) = ApiValues(mapOf(*pairs))

        fun ofNotNull(vararg pairs: Pair<String, Any?>) =
            ApiValues(pairs.filter { it.second != null }.toMap())
    }
}

internal fun ApiValues.text(key: String): String? =
    if (containsKey(key)) get(key)?.toString() ?: "" else null

internal fun ApiValues.name(key: String): String? = text(key)?.let {
    it.trim().ifEmpty { throw IllegalArgumentException("$key must not be empty") }
}

internal fun ApiValues.number(key: String): Long? {
    if (!containsKey(key)) return null
    return when (val value = get(key)) {
        null -> 0L
        is Number -> value.toLong()
        is Boolean -> if (value) 1L else 0L
        is CharSequence -> value.toString().takeIf { it.isNotEmpty() }?.toLongOrNull()
            ?: if (value.isEmpty()) 0L else throw IllegalArgumentException("$key must be a number, was '$value'")
        else -> throw IllegalArgumentException("$key must be a number")
    }
}

internal data class ApiDate(val millis: Long, val allDay: Boolean?)

internal fun ApiValues.date(key: String): ApiDate? {
    if (!containsKey(key)) return null
    return when (val value = get(key)) {
        null -> ApiDate(0L, null)
        is Number -> ApiDate(value.toLong(), null)
        is CharSequence -> parseDate(key, value.toString())
        else -> throw IllegalArgumentException("$key must be epoch milliseconds or a local date")
    }
}

internal fun parseDate(key: String, text: String): ApiDate {
    if (text.isEmpty()) return ApiDate(0L, null)
    text.toLongOrNull()?.let { return ApiDate(it, null) }
    val zone = ZoneId.systemDefault()
    runCatching { LocalDate.parse(text) }.getOrNull()?.let {
        return ApiDate(it.atStartOfDay(zone).toInstant().toEpochMilli(), true)
    }
    runCatching { LocalDateTime.parse(text) }.getOrNull()?.let {
        return ApiDate(it.atZone(zone).toInstant().toEpochMilli(), false)
    }
    throw IllegalArgumentException(
        "$key must be epoch milliseconds, a local date like 2026-09-12, or a local date and time " +
            "like 2026-09-12T19:00:00, was '$text'"
    )
}

internal fun ApiValues.instant(key: String): Long? = date(key)?.millis

internal fun ApiValues.decimal(key: String): Double? {
    if (!containsKey(key)) return null
    return when (val value = get(key)) {
        null -> 0.0
        is Number -> value.toDouble()
        is CharSequence -> value.toString().toDoubleOrNull()
            ?: throw IllegalArgumentException("$key must be a number, was '$value'")
        else -> throw IllegalArgumentException("$key must be a number")
    }
}

internal fun ApiValues.flag(key: String): Boolean? {
    if (!containsKey(key)) return null
    return when (val value = get(key)) {
        null -> false
        is Boolean -> value
        is Number -> value.toLong() != 0L
        is CharSequence -> when (value.toString()) {
            "1", "true" -> true
            "0", "false", "" -> false
            else -> throw IllegalArgumentException("$key must be 0 or 1, was '$value'")
        }
        else -> throw IllegalArgumentException("$key must be 0 or 1")
    }
}

internal fun <T> ApiValues.enum(key: String, allowed: Map<String, T>, empty: T): T? {
    val value = text(key) ?: return null
    if (value.isEmpty()) return empty
    return allowed[value] ?: throw IllegalArgumentException(
        "Unknown value for $key: '$value'. Expected one of ${allowed.keys.joinToString("|")}"
    )
}

internal fun <T> ApiValues.requiredEnum(key: String, allowed: Map<String, T>): T {
    val value = text(key)?.takeIf { it.isNotEmpty() }
        ?: throw IllegalArgumentException("$key is required")
    return allowed[value] ?: throw IllegalArgumentException(
        "Unknown value for $key: '$value'. Expected one of ${allowed.keys.joinToString("|")}"
    )
}

internal fun ApiValues.reject(path: String, allowed: Set<String>) {
    keys.firstOrNull { it !in allowed }?.let {
        throw IllegalArgumentException(
            "'$it' is not writable on /$path here. Writable: ${allowed.sorted().joinToString(", ")}"
        )
    }
}
