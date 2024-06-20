package org.tasks.filters

import org.tasks.CommonParcelize
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion.Companion.and
import org.tasks.data.sql.Order.Companion.desc
import org.tasks.data.sql.QueryTemplate
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.minusDays
import org.tasks.time.startOfMinute

@CommonParcelize
data class RecentlyModifiedFilter(
    override val title: String,
) : Filter {
    override val icon: Int
        get() = 6 // CustomIcons.HISTORY

    override val sql: String
        get() = QueryTemplate()
            .where(
                and(
                    Task.DELETION_DATE.lte(0),
                    Task.MODIFICATION_DATE.gt(
                        currentTimeMillis().minusDays(1).startOfMinute()
                    )
                )
            )
            .orderBy(desc(Task.MODIFICATION_DATE))
            .toString()

    override fun supportsHiddenTasks() = false

    override fun supportsSubtasks() = false

    override fun supportsSorting() = false

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is RecentlyModifiedFilter
    }
}
