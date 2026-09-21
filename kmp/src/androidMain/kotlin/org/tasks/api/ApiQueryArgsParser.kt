package org.tasks.api

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import org.tasks.api.TasksContract.PARAM_LIMIT
import org.tasks.api.TasksContract.PARAM_OFFSET

private val SQL_ARGS = listOf(
    ContentResolver.QUERY_ARG_SQL_SELECTION,
    ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
    ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
    ContentResolver.QUERY_ARG_SQL_GROUP_BY,
    ContentResolver.QUERY_ARG_SQL_HAVING,
)

private val HANDLED_ARGS = setOf(
    ContentResolver.QUERY_ARG_LIMIT,
    ContentResolver.QUERY_ARG_OFFSET,
    ContentResolver.QUERY_ARG_SORT_COLUMNS,
    ContentResolver.QUERY_ARG_SORT_DIRECTION,
)

fun ApiQueryArgs.Companion.parse(
    uri: Uri,
    queryArgs: Bundle?,
    allowed: Collection<String>,
): ApiQueryArgs = ApiQueryArgs.build(allowed) {
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
            requireSortable(ContentResolver.QUERY_ARG_SORT_COLUMNS)
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
            put(ApiQueryArgs.SORT, columns.single())
        }
        if (bundle.containsKey(ContentResolver.QUERY_ARG_SORT_DIRECTION)) {
            requireSortable(ContentResolver.QUERY_ARG_SORT_DIRECTION)
            val direction = bundle.getInt(ContentResolver.QUERY_ARG_SORT_DIRECTION)
            put(
                ApiQueryArgs.SORT_DESC,
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
            putAll(key, bundle.flatten(key))
        }
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
