package org.tasks.api

import android.content.ContentResolver
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import androidx.room.useReaderConnection
import org.tasks.api.TasksContract.Accounts
import org.tasks.api.TasksContract.Alarms
import org.tasks.api.TasksContract.Lists
import org.tasks.api.TasksContract.Places
import org.tasks.api.TasksContract.Tags
import org.tasks.api.TasksContract.TaskTags
import org.tasks.api.TasksContract.Tasks
import org.tasks.data.db.Database

class ApiQueryEngine(
    private val database: Database,
) {
    internal suspend fun queryCollection(
        table: ApiTable,
        projection: Array<out String>?,
        args: ApiQueryArgs,
        resolver: ContentResolver?,
        notificationUri: Uri,
    ): Cursor {
        val parts = table.parts(args)
        val order = buildOrder(table, args)
        val rows = if (args.limit == 0 || parts.isEmpty()) {
            emptyList()
        } else {
            select(table, parts, order, args.limit, args.offset)
        }
        val total = when {
            parts.isEmpty() -> 0
            args.limit == 0 -> count(parts)
            rows.size in 1 until args.limit -> args.offset + rows.size
            rows.isEmpty() && args.offset == 0 -> 0
            else -> count(parts)
        }
        return cursor(table, projection, rows).apply {
            extras = Bundle().apply { putInt(ContentResolver.EXTRA_TOTAL_COUNT, total) }
            resolver?.let { setNotificationUri(it, notificationUri) }
        }
    }

    internal suspend fun queryItem(
        table: ApiTable,
        projection: Array<out String>?,
        id: Long,
        resolver: ContentResolver?,
        notificationUri: Uri,
    ): Cursor {

        val parts = table.sources.mapNotNull { source ->
            source.byId(id)?.let { source to it.prependBase(source) }
        }
        val rows = if (parts.isEmpty()) {
            emptyList()
        } else {
            select(table, parts, ORDER_BY_ID, limit = 1, offset = 0)
        }
        return cursor(table, projection, rows).apply {
            resolver?.let { setNotificationUri(it, notificationUri) }
        }
    }

    private suspend fun select(
        table: ApiTable,
        parts: List<Part>,
        order: String,
        limit: Int,
        offset: Int,
    ): List<Array<Any?>> {
        val statement = "${unionSql(parts)} ORDER BY $order LIMIT $limit OFFSET $offset"
        return database.useReaderConnection { transactor ->
            transactor.usePrepared(statement) { prepared ->
                parts.bindArgs().bindTo(prepared)
                buildList { while (prepared.step()) add(table.read(prepared)) }
            }
        }
    }

    private suspend fun count(parts: List<Part>): Int {
        val statement = "SELECT COUNT(*) FROM (${unionSql(parts)})"
        return database.useReaderConnection { transactor ->
            transactor.usePrepared(statement) { prepared ->
                parts.bindArgs().bindTo(prepared)
                if (prepared.step()) prepared.getInt(0) else 0
            }
        }
    }

    private fun cursor(
        table: ApiTable,
        projection: Array<out String>?,
        rows: List<Array<Any?>>,
    ): MatrixCursor {
        val names = projection?.filter { it in table.columns } ?: table.columns
        val indices = names.map { table.indexOf(it) }
        val cursor = MatrixCursor(names.toTypedArray(), rows.size)
        rows.forEach { row -> cursor.addRow(indices.map { row[it] }) }
        return cursor
    }

    companion object {

        internal fun buildWhere(table: ApiTable, args: ApiQueryArgs): SqlWhere {
            val where = SqlWhere()
            when (table.path) {
                Tasks.PATH -> where.tasks(args)
                TaskTags.PATH -> where.taskTags(args)
                Lists.PATH -> where.lists(args)
                Tags.PATH -> where.ids("tagdata._id", args, Tags.PARAM_ID)
                Places.PATH -> where.ids("places.place_id", args, Places.PARAM_ID)
                Accounts.PATH -> where.ids("caldav_accounts.cda_id", args, Accounts.PARAM_ID)
            }
            return where
        }

        internal fun buildOrder(table: ApiTable, args: ApiQueryArgs): String {
            if (table.path != Tasks.PATH) {

                return ORDER_BY_ID
            }
            val descending = when (val value = args.single(Tasks.PARAM_SORT_DESC)) {
                null, "0", "false" -> false
                "1", "true" -> true
                else -> throw IllegalArgumentException("${Tasks.PARAM_SORT_DESC} must be 0 or 1, was '$value'")
            }
            val direction = if (descending) "DESC" else "ASC"
            val sort = args.single(Tasks.PARAM_SORT)
                ?: return "$API_ID $direction"
            val key = when (sort) {
                Tasks.SORT_DUE -> "tasks.dueDate"
                Tasks.SORT_START -> "tasks.hideUntil"
                Tasks.SORT_CREATED -> "tasks.created"
                Tasks.SORT_MODIFIED -> "tasks.modified"
                Tasks.SORT_PRIORITY -> "tasks.importance"
                Tasks.SORT_TITLE -> "tasks.title COLLATE NOCASE"
                else -> throw IllegalArgumentException(
                    "Unknown sort '$sort'. Supported: ${Tasks.SORTS.joinToString("|")}"
                )
            }
            return "$key $direction, $API_ID ASC"
        }

        private fun SqlWhere.ids(column: String, args: ApiQueryArgs, param: String) = apply {
            if (args.has(param)) inLongs(column, args.longs(param))
        }

        private fun SqlWhere.range(column: String, args: ApiQueryArgs, before: String, after: String) = apply {
            args.long(before)?.let { and("$column < ?", it) }
            args.long(after)?.let { and("$column > ?", it) }
        }

        private fun SqlWhere.tasks(args: ApiQueryArgs) = apply {
            ids("tasks._id", args, Tasks.PARAM_ID)
            args.single(Tasks.PARAM_SEARCH)?.let { likeAny(listOf("tasks.title", "tasks.notes"), it) }
            if (args.has(Tasks.PARAM_PARENT)) inLongs("tasks.parent", args.longs(Tasks.PARAM_PARENT))
            args.single(Tasks.PARAM_COMPLETED)?.let {
                when (it) {
                    "1", "true" -> and("tasks.completed > 0")
                    "0", "false" -> and("tasks.completed = 0")
                    else -> throw IllegalArgumentException(
                        "${Tasks.PARAM_COMPLETED} must be 0 or 1, was '$it'"
                    )
                }
            }
            if (args.has(Tasks.PARAM_PRIORITY)) {
                inInts("tasks.importance", args.enums(Tasks.PARAM_PRIORITY, Priorities.FROM_API))
            }
            if (args.has(Tasks.PARAM_LIST)) {
                and(
                    "EXISTS (SELECT 1 FROM caldav_tasks" +
                            " INNER JOIN caldav_lists ON cdl_uuid = cd_calendar" +
                            " WHERE cd_task = tasks._id AND cd_deleted = 0" +
                            " AND cdl_id IN (${args.longs(Tasks.PARAM_LIST).joinToString(",")}))"
                )
            }
            if (args.has(Tasks.PARAM_TAG)) {
                and(
                    "EXISTS (SELECT 1 FROM tags" +
                            " INNER JOIN tagdata ON tagdata.remoteId = tags.tag_uid" +
                            " WHERE tags.task = tasks._id" +
                            " AND tagdata._id IN (${args.longs(Tasks.PARAM_TAG).joinToString(",")}))"
                )
            }
            if (args.has(Tasks.PARAM_PLACE)) {
                and(
                    "EXISTS (SELECT 1 FROM geofences" +
                            " INNER JOIN places ON places.uid = geofences.place" +
                            " WHERE geofences.task = tasks._id" +
                            " AND places.place_id IN (${args.longs(Tasks.PARAM_PLACE).joinToString(",")}))"
                )
            }
            range("tasks.dueDate", args, Tasks.PARAM_DUE_BEFORE, Tasks.PARAM_DUE_AFTER)
            range("tasks.hideUntil", args, Tasks.PARAM_START_BEFORE, Tasks.PARAM_START_AFTER)
            range("tasks.completed", args, Tasks.PARAM_COMPLETED_BEFORE, Tasks.PARAM_COMPLETED_AFTER)
            range("tasks.created", args, Tasks.PARAM_CREATED_BEFORE, Tasks.PARAM_CREATED_AFTER)
            range("tasks.modified", args, Tasks.PARAM_MODIFIED_BEFORE, Tasks.PARAM_MODIFIED_AFTER)
        }

        private fun SqlWhere.taskTags(args: ApiQueryArgs) = apply {
            if (args.has(TaskTags.PARAM_TAG)) {
                and(
                    "EXISTS (SELECT 1 FROM tagdata WHERE tagdata.remoteId = tags.tag_uid" +
                            " AND tagdata._id IN (${args.longs(TaskTags.PARAM_TAG).joinToString(",")}))"
                )
            }
        }

        private fun SqlWhere.lists(args: ApiQueryArgs) = apply {
            ids("caldav_lists.cdl_id", args, Lists.PARAM_ID)
            if (args.has(Lists.PARAM_ACCOUNT)) {
                and(
                    "EXISTS (SELECT 1 FROM caldav_accounts WHERE cda_uuid = caldav_lists.cdl_account" +
                            " AND cda_id IN (${args.longs(Lists.PARAM_ACCOUNT).joinToString(",")}))"
                )
            }
            if (args.has(Lists.PARAM_ACCESS)) {
                inInts(
                    "caldav_lists.cdl_access",
                    args.all(Lists.PARAM_ACCESS).flatMap { ListAccess.storedFor(it) }.distinct(),
                )
            }
        }
    }
}

internal fun List<Any>.bindTo(statement: androidx.sqlite.SQLiteStatement) {
    forEachIndexed { index, value ->
        val position = index + 1
        when (value) {
            is Long -> statement.bindLong(position, value)
            is Int -> statement.bindLong(position, value.toLong())
            is Double -> statement.bindDouble(position, value)
            is String -> statement.bindText(position, value)
            else -> throw IllegalArgumentException("Cannot bind ${value::class.java.simpleName}")
        }
    }
}

internal typealias Part = Pair<ApiSource, SqlWhere>

internal const val API_ID = "api_id"

internal const val ORDER_BY_ID = "$API_ID ASC"

internal fun ApiTable.parts(args: ApiQueryArgs): List<Part> = sources.mapNotNull { source ->
    val sourceWhere = source.filter(args) ?: return@mapNotNull null
    val where = SqlWhere()
        .apply { source.baseWhere?.let { and(it) } }
        .merge(ApiQueryEngine.buildWhere(this@parts, args))
        .merge(sourceWhere)
    source to where
}

internal fun SqlWhere.prependBase(source: ApiSource): SqlWhere =
    SqlWhere().apply { source.baseWhere?.let { and(it) } }.merge(this)

internal fun unionSql(parts: List<Part>): String = parts.joinToString(" UNION ALL ") { (source, where) ->
    "SELECT ${source.idExpression} AS $API_ID, ${source.select} FROM ${source.from}${where.sql()}"
}

internal fun List<Part>.bindArgs(): List<Any> = flatMap { it.second.args }
