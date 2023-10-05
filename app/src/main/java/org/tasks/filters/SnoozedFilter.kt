package org.tasks.filters

import com.todoroo.andlib.sql.Criterion.Companion.and
import com.todoroo.andlib.sql.Join.Companion.inner
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.Filter.Companion.NO_COUNT
import com.todoroo.astrid.api.FilterListItem
import com.todoroo.astrid.data.Task
import kotlinx.parcelize.Parcelize
import org.tasks.R
import org.tasks.data.Alarm

@Parcelize
data class SnoozedFilter(
    override val title: String,
    override var count: Int = NO_COUNT,
) : Filter {
    override val icon: Int
        get() = R.drawable.ic_snooze_white_24dp

    override val sql: String
        get() = QueryTemplate()
            .join(inner(Alarm.TABLE, Task.ID.eq(Alarm.TASK)))
            .where(
                and(
                    Task.DELETION_DATE.lte(0),
                    Alarm.TYPE.eq(Alarm.TYPE_SNOOZE)
                )
            )
            .toString()

    override fun supportsHiddenTasks() = false

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is SnoozedFilter
    }
}
