package org.tasks.filters

import com.todoroo.andlib.sql.Join
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterListItem
import com.todoroo.astrid.data.Task
import kotlinx.parcelize.Parcelize
import org.tasks.R
import org.tasks.notifications.Notification

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