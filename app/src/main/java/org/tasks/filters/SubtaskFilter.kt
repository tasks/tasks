package org.tasks.filters

import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion
import org.tasks.data.sql.QueryTemplate

@Parcelize
data class SubtaskFilter(
    private val parent: Long,
) : Filter {
    @IgnoredOnParcel
    override val title: String = "subtasks"

    @IgnoredOnParcel
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

    override fun areItemsTheSame(other: FilterListItem): Boolean =
        other is SubtaskFilter && parent == other.parent
}