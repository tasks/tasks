package org.tasks.filters

import com.todoroo.andlib.sql.Criterion
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterListItem
import com.todoroo.astrid.data.Task
import kotlinx.parcelize.Parcelize
import org.tasks.R

@Parcelize
data class TimerFilter(override val title: String?) : Filter {
    override val icon
        get() = R.drawable.ic_outline_timer_24px

    override val sql: String
        get() = QueryTemplate()
            .where(
                Criterion.and(
                    Task.TIMER_START.gt(0),
                    Task.DELETION_DATE.eq(0)
                )
            ).toString()

    override fun areItemsTheSame(other: FilterListItem) = other is TimerFilter
}