package org.tasks.mcp

import kotlinx.serialization.Serializable
import org.tasks.api.ApiPage
import org.tasks.api.ApiQueryEngine
import org.tasks.api.FilterQuery
import org.tasks.api.TaskRow
import org.tasks.api.TasksContract
import org.tasks.api.filterTasks
import org.tasks.data.dao.FilterDao
import org.tasks.data.entity.Filter

class DatabaseFiltersApi(
    private val filterDao: FilterDao,
    private val engine: ApiQueryEngine,
) {

    @Serializable
    data class FilterRow(
        val id: Long,
        val title: String,
        val icon: String?,
        val color: Int,
        val order: Int,
        val sql: String?,
    )

    suspend fun list(
        includeSql: Boolean = false,
        ids: List<Long> = emptyList(),
        limit: Int = TasksContract.DEFAULT_LIMIT,
        offset: Int = 0,
    ): ApiPage<FilterRow> {
        val all = filterDao.getFilters().map { it.toRow(includeSql) }
        val matched = if (ids.isEmpty()) all else all.filter { it.id in ids }
        return ApiPage(matched.drop(offset).take(limit), matched.size, limit, offset)
    }

    suspend fun tasks(
        filterId: Long,
        query: FilterQuery,
    ): ApiPage<TaskRow> {
        val filter = requireNotNull(filterDao.getById(filterId)) {
            "No filter with id $filterId - read list_filters to see which filters exist"
        }
        val sql = filter.sql
        require(!sql.isNullOrBlank()) {
            "Filter $filterId ('${filter.title.orEmpty()}') has no stored SQL, so it cannot be run"
        }
        return engine.filterTasks(sql, query)
    }

    private fun Filter.toRow(includeSql: Boolean) = FilterRow(
        id = id,
        title = title.orEmpty(),
        icon = icon,
        color = color ?: 0,
        order = order,
        sql = if (includeSql) sql else null,
    )
}
