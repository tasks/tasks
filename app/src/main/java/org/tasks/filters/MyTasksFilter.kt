package org.tasks.filters

import com.todoroo.andlib.sql.Criterion
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.api.AstridOrderingFilter
import com.todoroo.astrid.api.FilterListItem
import com.todoroo.astrid.data.Task
import kotlinx.parcelize.Parcelize
import org.tasks.data.TaskDao
import org.tasks.themes.CustomIcons

@Parcelize
data class MyTasksFilter(
    override val title: String,
    override var filterOverride: String? = null,
) : AstridOrderingFilter {
    override val icon: Int
        get() = CustomIcons.ALL_INBOX
    override val sql: String
        get() = QueryTemplate()
            .where(
                Criterion.and(
                    TaskDao.TaskCriteria.activeAndVisible(),
                    Task.PARENT.eq(0)
                )
            ).toString()

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is MyTasksFilter
    }
}
