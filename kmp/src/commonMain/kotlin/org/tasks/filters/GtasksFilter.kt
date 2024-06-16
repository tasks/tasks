package org.tasks.filters

import org.tasks.CommonParcelize
import org.tasks.data.GoogleTask
import org.tasks.data.NO_COUNT
import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion.Companion.and
import org.tasks.data.sql.Join.Companion.left
import org.tasks.data.sql.QueryTemplate

@CommonParcelize
data class GtasksFilter(
    val list: CaldavCalendar,
    override val count: Int = NO_COUNT,
) : Filter {
    override val title: String?
        get() = list.name

    override val sql: String
        get() = QueryTemplate()
            .join(left(CaldavTask.TABLE, Task.ID.eq(CaldavTask.TASK)))
            .where(
                and(
                    activeAndVisible(),
                    CaldavTask.DELETED.eq(0),
                    CaldavTask.CALENDAR.eq(list.uuid)
                )
            )
            .toString()

    override val valuesForNewTasks: String
        get() = mapToSerializedString(mapOf(GoogleTask.KEY to list.uuid!!))

    override val order: Int
        get() = list.order

    override val icon: Int
        get() = list.getIcon()!!

    override val tint: Int
        get() = list.color

    val account: String
        get() = list.account!!

    override fun supportsManualSort() = true

    val remoteId: String
        get() = list.uuid!!

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is GtasksFilter && list.id == other.list.id
    }
}
