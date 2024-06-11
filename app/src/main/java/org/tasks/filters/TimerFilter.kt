package org.tasks.filters

import org.tasks.data.sql.Criterion
import org.tasks.data.sql.QueryTemplate
import org.tasks.data.entity.Task
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