package org.tasks.filters

import org.jetbrains.compose.resources.getString
import org.tasks.CommonParcelize
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion
import org.tasks.data.sql.QueryTemplate
import org.tasks.themes.TasksIcons
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.filter_timer

@CommonParcelize
data class TimerFilter(override val title: String) : Filter() {
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

    companion object {
        suspend fun create() = TimerFilter(getString(Res.string.filter_timer))
    }
}