package org.tasks.filters

import com.todoroo.astrid.api.PermaSql
import kotlinx.parcelize.Parcelize
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion
import org.tasks.data.sql.QueryTemplate
import org.tasks.themes.CustomIcons

@Parcelize
data class TodayFilter(
    override val title: String,
    override var filterOverride: String? = null,
) : AstridOrderingFilter {
    override val sql: String
        get() = QueryTemplate()
            .where(
                Criterion.and(
                    TaskDao.TaskCriteria.activeAndVisible(),
                    Task.DUE_DATE.gt(0),
                    Task.DUE_DATE.lte(PermaSql.VALUE_EOD)
                )
            )
            .toString()
    override val icon: Int
        get() = CustomIcons.TODAY

    override val valuesForNewTasks: String
        get() = mapToSerializedString(mapOf(Task.DUE_DATE.name to PermaSql.VALUE_NOON))

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is TodayFilter
    }
}