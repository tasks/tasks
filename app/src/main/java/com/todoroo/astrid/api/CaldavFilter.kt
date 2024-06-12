package com.todoroo.astrid.api

import org.tasks.data.sql.Criterion.Companion.and
import org.tasks.data.sql.Join.Companion.left
import org.tasks.data.sql.QueryTemplate
import com.todoroo.andlib.utility.AndroidUtilities
import kotlinx.parcelize.Parcelize
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.NO_COUNT
import org.tasks.data.entity.Task
import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.filters.Filter
import org.tasks.filters.FilterListItem
import org.tasks.filters.mapToSerializedString

@Parcelize
data class CaldavFilter(
    val calendar: CaldavCalendar,
    val principals: Int = 0,
    override val count: Int = NO_COUNT,
) : Filter {
    override val title: String?
        get() = calendar.name
    override val sql: String
        get() = QueryTemplate()
            .join(left(CaldavTask.TABLE, Task.ID.eq(CaldavTask.TASK)))
            .where(
                and(
                    activeAndVisible(),
                    CaldavTask.DELETED.eq(0),
                    CaldavTask.CALENDAR.eq(calendar.uuid)
                )
            )
            .toString()
    override val valuesForNewTasks: String
        get() = mapToSerializedString(mapOf(CaldavTask.KEY to calendar.uuid!!))

    override val order: Int
        get() = calendar.order

    override val tint: Int
        get() = calendar.color
    override val icon: Int
        get() = calendar.getIcon()!!

    val uuid: String
        get() = calendar.uuid!!
    val account: String
        get() = calendar.account!!

    override val isReadOnly: Boolean
        get() = calendar.access == CaldavCalendar.ACCESS_READ_ONLY

    override fun supportsManualSort() = true

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is CaldavFilter && calendar.id == other.calendar.id
    }
}
