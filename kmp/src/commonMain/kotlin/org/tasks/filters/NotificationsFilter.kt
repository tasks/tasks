package org.tasks.filters

import org.jetbrains.compose.resources.getString
import org.tasks.CommonParcelize
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Task
import org.tasks.data.sql.Join
import org.tasks.data.sql.QueryTemplate
import org.tasks.themes.TasksIcons
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.filter_notifications

@CommonParcelize
data class NotificationsFilter(
    override val title: String,
) : Filter {
    override val icon: String
        get() = TasksIcons.NOTIFICATIONS

    override val sql: String
        get() = QueryTemplate()
            .join(Join.inner(Notification.TABLE, Task.ID.eq(Notification.TASK)))
            .toString()

    override fun supportsHiddenTasks(): Boolean = false

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is NotificationsFilter
    }

    companion object {
        suspend fun create() = NotificationsFilter(getString(Res.string.filter_notifications))
    }
}