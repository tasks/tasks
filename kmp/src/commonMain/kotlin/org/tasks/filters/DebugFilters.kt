package org.tasks.filters

import org.tasks.CommonParcelize
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion.Companion.and
import org.tasks.data.sql.Criterion.Companion.or
import org.tasks.data.sql.Join
import org.tasks.data.sql.QueryTemplate
import org.tasks.themes.TasksIcons

@CommonParcelize
data class DebugFilter(
    override val title: String,
    override val sql: String?,
    override val icon: String?,
) : Filter() {
    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is Filter && sql == other.sql
    }
}

object DebugFilters {
    fun getNoListFilter() =
        DebugFilter(
            title = "No list",
            sql = QueryTemplate()
                .join(Join.left(CaldavTask.TABLE, CaldavTask.TASK.eq(Task.ID)))
                .where(CaldavTask.ID.eq(null))
                .toString(),
            icon = TasksIcons.CLOUD_OFF,
        )

    fun getDeleted() =
        DebugFilter(
            title = "Deleted",
            sql = QueryTemplate().where(Task.DELETION_DATE.gt(0)).toString(),
            icon = TasksIcons.DELETE,
        )

    fun getMissingListFilter() =
        DebugFilter(
            title = "Missing list",
            sql = QueryTemplate()
                .join(Join.left(CaldavTask.TABLE, CaldavTask.TASK.eq(Task.ID)))
                .join(
                    Join.left(
                        CaldavCalendar.TABLE,
                        CaldavCalendar.UUID.eq(CaldavTask.CALENDAR)
                    )
                )
                .where(and(CaldavTask.ID.gt(0), CaldavCalendar.UUID.eq(null)))
                .toString(),
            icon = TasksIcons.CLOUD_OFF,
        )

    fun getMissingAccountFilter() =
        DebugFilter(
            title = "Missing account",
            sql = QueryTemplate()
                .join(
                    Join.left(CaldavTask.TABLE, and(CaldavTask.TASK.eq(Task.ID)))
                ).join(
                    Join.left(CaldavCalendar.TABLE, CaldavCalendar.UUID.eq(CaldavTask.CALENDAR))
                ).join(
                    Join.left(
                        CaldavAccount.TABLE, CaldavAccount.UUID.eq(CaldavCalendar.ACCOUNT)
                    )
                )
                .where(and(CaldavTask.ID.gt(0), CaldavAccount.UUID.eq(null)))
                .toString(),
            icon = TasksIcons.CLOUD_OFF,
        )

    fun getNoTitleFilter() =
        DebugFilter(
            title = "No title",
            sql = QueryTemplate().where(or(Task.TITLE.eq(null), Task.TITLE.eq(""))).toString(),
            icon = TasksIcons.CLEAR,
        )

    fun getNoCreateDateFilter() =
        DebugFilter(
            title = "No create time",
            sql = QueryTemplate().where(Task.CREATION_DATE.eq(0)).toString(),
            icon = TasksIcons.ADD,
        )

    fun getNoModificationDateFilter() =
        DebugFilter(
            title = "No modify time",
            sql = QueryTemplate().where(Task.MODIFICATION_DATE.eq(0)).toString(),
            icon = TasksIcons.EDIT,
        )
}