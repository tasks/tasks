package org.tasks.api

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
