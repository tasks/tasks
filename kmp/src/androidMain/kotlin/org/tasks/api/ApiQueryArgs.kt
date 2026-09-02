package org.tasks.api

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
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

    fun <T> enums(key: String, allowed: Map<String, T>): List<T> = all(key).map {
        allowed[it] ?: throw IllegalArgumentException(
            "Unknown value for $key: '$it'. Expected one of ${allowed.keys.joinToString("|")}"
        )
    }

    private fun String.toIntOrNull(key: String): Int =
        toIntOrNull() ?: throw IllegalArgumentException("$key must be an integer, was '$this'")

    private fun String.toLongOrThrow(key: String): Long =
        toLongOrNull() ?: throw IllegalArgumentException("$key must be a number, was '$this'")

    companion object {
        private val SQL_ARGS = listOf(
            ContentResolver.QUERY_ARG_SQL_SELECTION,
            ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
            ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
            ContentResolver.QUERY_ARG_SQL_GROUP_BY,
            ContentResolver.QUERY_ARG_SQL_HAVING,
        )

        private const val SORT = "sort"
        private const val SORT_DESC = "sort_desc"

        private val HANDLED_ARGS = setOf(
            ContentResolver.QUERY_ARG_LIMIT,
            ContentResolver.QUERY_ARG_OFFSET,
            ContentResolver.QUERY_ARG_SORT_COLUMNS,
            ContentResolver.QUERY_ARG_SORT_DIRECTION,
        )

        private fun requireSortable(allowed: Collection<String>, arg: String) {
            if (SORT !in allowed) {
                throw IllegalArgumentException("$arg is not supported here; this collection sorts by $ID")
            }
        }

        fun parse(uri: Uri, queryArgs: Bundle?, allowed: Collection<String>): ApiQueryArgs {
            val values = LinkedHashMap<String, MutableList<String>>()

            fun put(key: String, value: String) {
                if (key !in allowed) {
                    throw IllegalArgumentException(
                        "Unknown parameter '$key'. Supported: ${allowed.sorted().joinToString(", ")}"
                    )
                }
                values.getOrPut(key) { mutableListOf() }.add(value)
            }

            if (uri.isOpaque) {
                throw IllegalArgumentException("Unsupported URI: $uri")
            }
            for (key in uri.queryParameterNames) {
                for (value in uri.getQueryParameters(key)) {
                    put(key, value)
                }
            }
            queryArgs?.let { bundle ->
                SQL_ARGS.firstOrNull { bundle.containsKey(it) }?.let {
                    throw IllegalArgumentException(
                        "$it is not supported. Filter with the documented query parameters."
                    )
                }
                if (bundle.containsKey(ContentResolver.QUERY_ARG_LIMIT)) {
                    put(PARAM_LIMIT, bundle.getInt(ContentResolver.QUERY_ARG_LIMIT).toString())
                }
                if (bundle.containsKey(ContentResolver.QUERY_ARG_OFFSET)) {
                    put(PARAM_OFFSET, bundle.getInt(ContentResolver.QUERY_ARG_OFFSET).toString())
                }
                if (bundle.containsKey(ContentResolver.QUERY_ARG_SORT_COLUMNS)) {
                    requireSortable(allowed, ContentResolver.QUERY_ARG_SORT_COLUMNS)
                    val columns = bundle.getStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS)
                        ?: throw IllegalArgumentException(
                            "${ContentResolver.QUERY_ARG_SORT_COLUMNS} must be a String[]"
                        )
                    if (columns.size != 1) {
                        throw IllegalArgumentException(
                            "${ContentResolver.QUERY_ARG_SORT_COLUMNS} takes exactly one column;" +
                                    " this API sorts on one key, with _id as the tiebreaker"
                        )
                    }
                    put(SORT, columns.single())
                }
                if (bundle.containsKey(ContentResolver.QUERY_ARG_SORT_DIRECTION)) {
                    requireSortable(allowed, ContentResolver.QUERY_ARG_SORT_DIRECTION)
                    val direction = bundle.getInt(ContentResolver.QUERY_ARG_SORT_DIRECTION)
                    put(
                        SORT_DESC,
                        when (direction) {
                            ContentResolver.QUERY_SORT_DIRECTION_ASCENDING -> "0"
                            ContentResolver.QUERY_SORT_DIRECTION_DESCENDING -> "1"
                            else -> throw IllegalArgumentException(
                                "Unknown ${ContentResolver.QUERY_ARG_SORT_DIRECTION}: $direction"
                            )
                        },
                    )
                }
                for (key in bundle.keySet()) {
                    if (key in HANDLED_ARGS) {
                        continue
                    }
                    bundle.flatten(key).forEach { put(key, it) }
                }
            }
            return ApiQueryArgs(values)
        }

        fun rejectSql(selection: String?, selectionArgs: Array<out String>?, sortOrder: String? = null) {
            if (selection != null || selectionArgs != null) {
                throw IllegalArgumentException(
                    "selection/selectionArgs are not supported. Filter with the documented query parameters."
                )
            }
            if (sortOrder != null) {
                throw IllegalArgumentException("sortOrder is not supported. Use the sort parameter.")
            }
        }

        @Suppress("DEPRECATION")
        private fun Bundle.flatten(key: String): List<String> = when (val value = get(key)) {
            null -> throw IllegalArgumentException("$key must not be null")
            is String -> listOf(value)
            is Int, is Long, is Boolean -> listOf(value.toString())
            is Array<*> -> value.map { it?.toString() ?: throw IllegalArgumentException("$key must not contain null") }
            is IntArray -> value.map { it.toString() }
            is LongArray -> value.map { it.toString() }
            is Iterable<*> -> value.map { it?.toString() ?: throw IllegalArgumentException("$key must not contain null") }
            else -> throw IllegalArgumentException("Unsupported value for $key: ${value::class.java.simpleName}")
        }
    }
}
