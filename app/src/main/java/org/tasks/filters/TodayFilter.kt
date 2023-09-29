package org.tasks.filters

import com.todoroo.andlib.sql.Criterion
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.api.AstridOrderingFilter
import com.todoroo.astrid.api.FilterListItem
import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.data.Task
import kotlinx.parcelize.Parcelize
import org.tasks.data.TaskDao
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
        get() = AndroidUtilities.mapToSerializedString(mapOf(Task.DUE_DATE.name to PermaSql.VALUE_NOON))

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is TodayFilter
    }
}