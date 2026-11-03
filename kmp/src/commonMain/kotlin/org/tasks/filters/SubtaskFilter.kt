package org.tasks.filters

import org.tasks.CommonIgnoredOnParcel
import org.tasks.CommonParcelize
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion
import org.tasks.data.sql.QueryTemplate

@CommonParcelize
data class SubtaskFilter(
    private val parent: Long,
    val calendarUuid: String? = null,
    val isGoogleTasks: Boolean = false,
) : Filter() {
    @CommonIgnoredOnParcel
    override val title: String = "subtasks"

    @CommonIgnoredOnParcel
    override val sql: String =
        QueryTemplate()
            .where(
                Criterion.and(
                    TaskDao.TaskCriteria.activeAndVisible(),
                    Task.PARENT.eq(parent)
                )
            )
            .toString()

    override fun disableHeaders() = true
    
    // Manual sort is supported when the subtask belongs to a CalDAV calendar,
    // allowing it to use the same ordering as the parent task list
    override fun supportsManualSort(): Boolean = calendarUuid != null

    override fun areItemsTheSame(other: FilterListItem): Boolean =
        other is SubtaskFilter && parent == other.parent
}