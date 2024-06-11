package org.tasks.filters

import org.tasks.data.sql.Join
import org.tasks.data.sql.QueryTemplate
import org.tasks.data.entity.Task
import kotlinx.parcelize.Parcelize
import org.tasks.R
import org.tasks.data.entity.Notification

@Parcelize
data class NotificationsFilter(
    override val title: String,
) : Filter {
    override val icon: Int
        get() = R.drawable.ic_outline_notifications_24px
    override val sql: String
        get() = QueryTemplate()
            .join(Join.inner(Notification.TABLE, Task.ID.eq(Notification.TASK)))
            .toString()

    override fun supportsHiddenTasks(): Boolean = false

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is NotificationsFilter
    }
}