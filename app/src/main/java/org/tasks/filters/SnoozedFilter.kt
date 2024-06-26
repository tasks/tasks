package org.tasks.filters

import org.tasks.data.sql.Criterion.Companion.and
import org.tasks.data.sql.Functions.now
import org.tasks.data.sql.Join.Companion.inner
import org.tasks.data.sql.QueryTemplate
import org.tasks.data.entity.Task
import kotlinx.parcelize.Parcelize
import org.tasks.data.entity.Alarm
import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.themes.TasksIcons

@Parcelize
data class SnoozedFilter(
    override val title: String,
) : Filter {
    override val icon: String
        get() = TasksIcons.SNOOZE

    override val sql: String
        get() = QueryTemplate()
            .join(inner(Alarm.TABLE, Task.ID.eq(Alarm.TASK)))
            .where(
                and(
                    activeAndVisible(),
                    Alarm.TYPE.eq(Alarm.TYPE_SNOOZE),
                    Alarm.TIME.gt(now()),
                )
            )
            .toString()

    override fun supportsHiddenTasks() = false

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is SnoozedFilter
    }
}
