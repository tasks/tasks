package org.tasks.api

import org.tasks.api.TasksContract.ID
import org.tasks.api.TasksContract.PARAM_LIMIT
import org.tasks.api.TasksContract.PARAM_OFFSET

class ApiQueryArgs private constructor(
    private val values: Map<String, List<String>>,
) {
    val limit: Int = single(PARAM_LIMIT)?.toIntOrNull(PARAM_LIMIT)?.also {
        if (it < 0) throw IllegalArgumentException("$PARAM_LIMIT must not be negative")
    } ?: TasksContract.DEFAULT_LIMIT

    val offset: Int = single(PARAM_OFFSET)?.toIntOrNull(PARAM_OFFSET)?.also {
        if (it < 0) throw IllegalArgumentException("$PARAM_OFFSET must not be negative")
    } ?: 0

    fun has(key: String) = values.containsKey(key)

    fun all(key: String): List<String> = values[key].orEmpty()

    fun single(key: String): String? = values[key]?.lastOrNull()

    fun longs(key: String): List<Long> = all(key).map { it.toLongOrThrow(key) }

    fun long(key: String): Long? = single(key)?.toLongOrThrow(key)

    fun instant(key: String): Long? = single(key)?.let { parseDate(key, it).millis }

    fun flag(key: String): Boolean = when (val value = single(key)) {
        null, "0", "false" -> false
        "1", "true" -> true
        else -> throw IllegalArgumentException("$key must be 0 or 1, was '$value'")
    }

    fun <T> enums(key: String, allowed: Map<String, T>): List<T> = all(key).map {
        allowed[it] ?: throw IllegalArgumentException(
            "Unknown value for $key: '$it'. Expected one of ${allowed.keys.joinToString("|")}"
        )
    }

    private fun String.toIntOrNull(key: String): Int =
        toIntOrNull() ?: throw IllegalArgumentException("$key must be an integer, was '$this'")

    private fun String.toLongOrThrow(key: String): Long =
        toLongOrNull() ?: throw IllegalArgumentException("$key must be a number, was '$this'")

    class Builder(private val allowed: Collection<String>) {
        private val values = LinkedHashMap<String, MutableList<String>>()

        fun put(key: String, value: String) = apply {
            if (key !in allowed) {
                throw IllegalArgumentException(
                    "Unknown parameter '$key'. Supported: ${allowed.sorted().joinToString(", ")}"
                )
            }
            values.getOrPut(key) { mutableListOf() }.add(value)
        }

        fun putAll(key: String, values: Iterable<String>) = apply {
            values.forEach { put(key, it) }
        }

        fun requireSortable(arg: String) {
            if (SORT !in allowed) {
                throw IllegalArgumentException(
                    "$arg is not supported here; this collection sorts by $ID"
                )
            }
        }

        fun build() = ApiQueryArgs(values)
    }

    companion object {
        internal const val SORT = "sort"
        internal const val SORT_DESC = "sort_desc"

        fun build(allowed: Collection<String>, block: Builder.() -> Unit): ApiQueryArgs =
            Builder(allowed).apply(block).build()

        fun rejectSql(
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String? = null,
        ) {
            if (selection != null || selectionArgs != null) {
                throw IllegalArgumentException(
                    "selection/selectionArgs are not supported. Filter with the documented query parameters."
                )
            }
            if (sortOrder != null) {
                throw IllegalArgumentException("sortOrder is not supported. Use the sort parameter.")
            }
        }
    }
}
