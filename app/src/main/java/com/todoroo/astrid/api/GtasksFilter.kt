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
import org.tasks.data.GoogleTask
import org.tasks.data.TaskDao.TaskCriteria.activeAndVisible

@Parcelize
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
        get() = AndroidUtilities.mapToSerializedString(mapOf(GoogleTask.KEY to list.uuid!!))

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
