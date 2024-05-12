package org.tasks.filters

import org.tasks.data.sql.Criterion.Companion.and
import org.tasks.data.sql.Order.Companion.desc
import org.tasks.data.sql.QueryTemplate
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterListItem
import org.tasks.data.entity.Task
import kotlinx.parcelize.Parcelize
import org.tasks.themes.CustomIcons
import org.tasks.time.DateTime

@Parcelize
data class RecentlyModifiedFilter(
    override val title: String,
) : Filter {
    override val icon: Int
        get() = CustomIcons.HISTORY

    override val sql: String
        get() = QueryTemplate()
            .where(
                and(
                    Task.DELETION_DATE.lte(0),
                    Task.MODIFICATION_DATE.gt(
                        DateTime().minusDays(1).startOfMinute().millis
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
