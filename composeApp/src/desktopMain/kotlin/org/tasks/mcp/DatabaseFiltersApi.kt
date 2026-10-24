package org.tasks.mcp

import kotlinx.serialization.Serializable
import org.tasks.api.ApiPage
import org.tasks.api.ApiQueryEngine
import org.tasks.api.FilterQuery
import org.tasks.api.TaskRow
import org.tasks.api.TasksContract
import org.tasks.api.filterTasks
import org.tasks.data.NO_ORDER
import org.tasks.data.dao.FilterDao
import org.tasks.data.entity.Filter
import org.tasks.filters.CaldavFilter
import org.tasks.filters.CustomFilter
import org.tasks.filters.FilterPreferenceCodec
import org.tasks.filters.PlaceFilter
import org.tasks.filters.TagFilter
import org.tasks.filters.key
import org.tasks.preferences.TasksPreferences

class DatabaseFiltersApi(
    private val filterDao: FilterDao,
    private val engine: ApiQueryEngine,
    private val preferences: TasksPreferences,
    private val codec: FilterPreferenceCodec,
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

    /**
     * The filter the user currently has selected in the app's navigation drawer. The app
     * mirrors every drawer selection into [TasksPreferences.lastViewedList], encoded by
     * [FilterPreferenceCodec], so reading and decoding that preference gives the live
     * selection without touching the UI. Search and the empty view are never recorded, so
     * while the user is searching this reports the last filter they selected.
     */
    suspend fun selected(includeSql: Boolean = false): SelectedFilter? {
        val stored = preferences.get(TasksPreferences.lastViewedList, "")
        if (stored.isEmpty()) return null
        return when (val filter = codec.decode(stored) ?: return null) {
            is CustomFilter -> SelectedFilter(
                type = "custom_filter",
                id = filter.id,
                title = filter.title,
                icon = filter.icon,
                color = filter.tint,
                order = filter.order,
                sql = if (includeSql) filter.sql else null,
            )
            is CaldavFilter -> SelectedFilter(
                type = "list",
                id = filter.calendar.id,
                title = filter.title,
                icon = filter.icon,
                color = filter.tint,
                order = filter.order,
            )
            is TagFilter -> SelectedFilter(
                type = "tag",
                id = filter.tagData.id,
                title = filter.title,
                icon = filter.icon,
                color = filter.tint,
                order = filter.order,
            )
            is PlaceFilter -> SelectedFilter(
                type = "place",
                id = filter.place.id,
                title = filter.title,
                icon = filter.icon,
                color = filter.tint,
                order = filter.order,
            )
            else -> SelectedFilter(
                type = "builtin",
                title = filter.title,
                key = filter.key(),
            )
        }
    }

    @Serializable
    data class SelectedFilter(
        val type: String,
        val id: Long? = null,
        val title: String,
        val icon: String? = null,
        val color: Int = 0,
        val order: Int = NO_ORDER,
        val key: String? = null,
        val sql: String? = null,
    )

    private fun Filter.toRow(includeSql: Boolean) = FilterRow(
        id = id,
        title = title.orEmpty(),
        icon = icon,
        color = color ?: 0,
        order = order,
        sql = if (includeSql) sql else null,
    )
}
