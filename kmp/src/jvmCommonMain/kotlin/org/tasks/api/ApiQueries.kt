package org.tasks.api

import org.tasks.api.TasksContract.Accounts
import org.tasks.api.TasksContract.Reminders
import org.tasks.api.TasksContract.Lists
import org.tasks.api.TasksContract.Places
import org.tasks.api.TasksContract.Tags

data class ApiPage<T>(
    val rows: List<T>,
    val total: Int,
    val offset: Int?,
) {
    val hasMore: Boolean? get() = offset?.let { it + rows.size < total }
}

fun <T> ApiPage(rows: List<T>, total: Int, limit: Int, offset: Int): ApiPage<T> =
    ApiPage(rows, total, offset.takeIf { limit > 0 })

fun pageLimit(limit: Int?): Int =
    (limit ?: TasksContract.DEFAULT_LIMIT).coerceIn(0, TasksContract.MAX_LIMIT)

fun pageOffset(offset: Int?): Int = (offset ?: 0).coerceAtLeast(0)

data class ListQuery(
    val ids: List<Long> = emptyList(),
    val accountIds: List<Long> = emptyList(),
    val access: List<String> = emptyList(),
    val limit: Int? = null,
    val offset: Int? = null,
)

data class TagQuery(
    val ids: List<Long> = emptyList(),
    val limit: Int? = null,
    val offset: Int? = null,
)

data class PlaceQuery(
    val ids: List<Long> = emptyList(),
    val limit: Int? = null,
    val offset: Int? = null,
)

data class ReminderQuery(
    val taskIds: List<Long> = emptyList(),
    val types: List<String> = emptyList(),
    val placeIds: List<Long> = emptyList(),
    val limit: Int? = null,
    val offset: Int? = null,
)

data class AccountQuery(
    val ids: List<Long> = emptyList(),
    val limit: Int? = null,
    val offset: Int? = null,
)

suspend fun ApiQueryEngine.findLists(query: ListQuery): ApiPage<ListRow> =
    find(Lists.PATH, query.limit, query.offset, { it.toListRow() }) {
        putEach(Lists.PARAM_ID, query.ids)
        putEach(Lists.PARAM_ACCOUNT, query.accountIds)
        putEach(Lists.PARAM_ACCESS, query.access)
    }

suspend fun ApiQueryEngine.findTags(query: TagQuery): ApiPage<TagRow> =
    find(Tags.PATH, query.limit, query.offset, { it.toTagRow() }) {
        putEach(Tags.PARAM_ID, query.ids)
    }

suspend fun ApiQueryEngine.findPlaces(query: PlaceQuery): ApiPage<PlaceRow> =
    find(Places.PATH, query.limit, query.offset, { it.toPlaceRow() }) {
        putEach(Places.PARAM_ID, query.ids)
    }

suspend fun ApiQueryEngine.findReminders(query: ReminderQuery): ApiPage<ReminderRow> =
    find(Reminders.PATH, query.limit, query.offset, { it.toReminderRow() }) {
        putEach(Reminders.PARAM_TASK, query.taskIds)
        putEach(Reminders.PARAM_TYPE, query.types)
        putEach(Reminders.PARAM_PLACE, query.placeIds)
    }

suspend fun ApiQueryEngine.findAccounts(query: AccountQuery): ApiPage<AccountRow> =
    find(Accounts.PATH, query.limit, query.offset, { it.toAccountRow() }) {
        putEach(Accounts.PARAM_ID, query.ids)
    }

private suspend fun <T> ApiQueryEngine.find(
    path: String,
    limit: Int?,
    offset: Int?,
    map: (ApiRow) -> T,
    filters: ApiQueryArgs.Builder.() -> Unit,
): ApiPage<T> {
    val take = pageLimit(limit)
    val skip = pageOffset(offset)
    val args = ApiQueryArgs.build(TasksContract.paramsFor(path)) {
        filters()
        put(TasksContract.PARAM_LIMIT, take.toString())
        put(TasksContract.PARAM_OFFSET, skip.toString())
    }
    val rows = query(path, args)
    return ApiPage(rows.map { map(it) }, rows.total, take, skip)
}

internal fun ApiQueryArgs.Builder.putEach(key: String, values: Collection<Any>) {
    values.forEach { put(key, it.toString()) }
}

internal fun ApiQueryArgs.Builder.putIfNotNull(key: String, value: Any?) {
    if (value != null) put(key, value.toString())
}

suspend fun ApiQueryEngine.tasksById(ids: List<Long>): List<TaskRow> {
    if (ids.isEmpty()) return emptyList()
    val found = findTasks(TaskQuery(ids = ids, status = "any", limit = ids.size))
        .rows
        .associateBy { it.id }
    return ids.mapNotNull { found[it] }
}
