package org.tasks.filters

import org.jetbrains.compose.resources.getString
import org.tasks.CommonParcelize
import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion.Companion.and
import org.tasks.data.sql.Functions.now
import org.tasks.data.sql.Join.Companion.inner
import org.tasks.data.sql.QueryTemplate
import org.tasks.themes.TasksIcons
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.filter_snoozed

@CommonParcelize
data class SnoozedFilter(
    override val title: String,
) : Filter() {
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

    companion object {
        suspend fun create() = SnoozedFilter(getString(Res.string.filter_snoozed))
    }
}
