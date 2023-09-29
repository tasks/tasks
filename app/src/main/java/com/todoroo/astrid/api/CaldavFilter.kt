package com.todoroo.astrid.api

import com.todoroo.andlib.sql.Criterion.Companion.and
import com.todoroo.andlib.sql.Join.Companion.left
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.api.Filter.Companion.NO_COUNT
import com.todoroo.astrid.data.Task
import kotlinx.parcelize.Parcelize
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavTask
import org.tasks.data.TaskDao.TaskCriteria.activeAndVisible

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
        get() = AndroidUtilities.mapToSerializedString(mapOf(CaldavTask.KEY to calendar.uuid!!))

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
