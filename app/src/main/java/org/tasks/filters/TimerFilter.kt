package org.tasks.filters

import org.tasks.data.sql.Criterion
import org.tasks.data.sql.QueryTemplate
import org.tasks.data.entity.Task
import kotlinx.parcelize.Parcelize
import org.tasks.themes.TasksIcons

@Parcelize
data class TimerFilter(override val title: String?) : Filter {
    override val icon
        get() = TasksIcons.TIMER

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