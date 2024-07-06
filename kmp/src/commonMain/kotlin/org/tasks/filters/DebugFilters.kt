package org.tasks.filters

import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion.Companion.and
import org.tasks.data.sql.Criterion.Companion.or
import org.tasks.data.sql.Join
import org.tasks.data.sql.QueryTemplate
import org.tasks.themes.TasksIcons

object DebugFilters {
    fun getNoListFilter() =
        FilterImpl(
            title = "No list",
            sql = QueryTemplate()
                .join(Join.left(CaldavTask.TABLE, CaldavTask.TASK.eq(Task.ID)))
                .where(CaldavTask.ID.eq(null))
                .toString(),
            icon = TasksIcons.CLOUD_OFF,
        )

    fun getDeleted() =
        FilterImpl(
            title = "Deleted",
            sql = QueryTemplate().where(Task.DELETION_DATE.gt(0)).toString(),
            icon = TasksIcons.DELETE,
        )

    fun getMissingListFilter() =
        FilterImpl(
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
        FilterImpl(
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
        FilterImpl(
            title = "No title",
            sql = QueryTemplate().where(or(Task.TITLE.eq(null), Task.TITLE.eq(""))).toString(),
            icon = TasksIcons.CLEAR,
        )

    fun getNoCreateDateFilter() =
        FilterImpl(
            title = "No create time",
            sql = QueryTemplate().where(Task.CREATION_DATE.eq(0)).toString(),
            icon = TasksIcons.ADD,
        )

    fun getNoModificationDateFilter() =
        FilterImpl(
            title = "No modify time",
            sql = QueryTemplate().where(Task.MODIFICATION_DATE.eq(0)).toString(),
            icon = TasksIcons.EDIT,
        )
}