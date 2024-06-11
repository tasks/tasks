/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core

import android.content.Context
import android.content.res.Resources
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion.Companion.and
import org.tasks.data.sql.Criterion.Companion.or
import org.tasks.data.sql.Join
import org.tasks.data.sql.QueryTemplate
import org.tasks.filters.Filter
import org.tasks.filters.FilterImpl
import org.tasks.filters.MyTasksFilter
import org.tasks.filters.NotificationsFilter
import org.tasks.filters.RecentlyModifiedFilter
import org.tasks.filters.SnoozedFilter
import org.tasks.filters.TimerFilter
import org.tasks.filters.TodayFilter
import org.tasks.preferences.Preferences
import javax.inject.Inject

class BuiltInFilterExposer @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val preferences: Preferences,
        private val taskDao: TaskDao
) {

    val myTasksFilter: Filter
        get() = getMyTasksFilter(context.resources)

    suspend fun filters(): List<Filter> {
        val r = context.resources
        val filters: MutableList<Filter> = ArrayList()
        if (preferences.getBoolean(R.string.p_show_today_filter, true)) {
            filters.add(getTodayFilter(r))
        }
        if (preferences.getBoolean(R.string.p_show_recently_modified_filter, true)) {
            filters.add(getRecentlyModifiedFilter(r))
        }
        if (taskDao.snoozedReminders() > 0) {
            filters.add(getSnoozedFilter(r))
        }
        if (taskDao.activeTimers() > 0) {
            filters.add(getTimerFilter(r))
        }
        if (taskDao.hasNotifications() > 0) {
            filters.add(getNotificationsFilter(context))
        }
        return filters
    }

    companion object {
        fun getMyTasksFilter(r: Resources) = MyTasksFilter(r.getString(R.string.BFE_Active))

        fun getTodayFilter(r: Resources): Filter {
            return TodayFilter(r.getString(R.string.today))
        }

        fun getNoListFilter() =
            FilterImpl(
                title = "No list",
                sql = QueryTemplate()
                    .join(Join.left(CaldavTask.TABLE, CaldavTask.TASK.eq(Task.ID)))
                    .where(CaldavTask.ID.eq(null))
                    .toString(),
                icon = R.drawable.ic_outline_cloud_off_24px,
            )

        fun getDeleted() =
            FilterImpl(
                title = "Deleted",
                sql = QueryTemplate().where(Task.DELETION_DATE.gt(0)).toString(),
                icon = R.drawable.ic_outline_delete_24px,
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
                icon = R.drawable.ic_outline_cloud_off_24px,
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
                icon = R.drawable.ic_outline_cloud_off_24px,
            )

        fun getNoTitleFilter() =
            FilterImpl(
                title = "No title",
                sql = QueryTemplate().where(or(Task.TITLE.eq(null), Task.TITLE.eq(""))).toString(),
                icon = R.drawable.ic_outline_clear_24px,
            )

        fun getNoCreateDateFilter() =
            FilterImpl(
                title = "No create time",
                sql = QueryTemplate().where(Task.CREATION_DATE.eq(0)).toString(),
                icon = R.drawable.ic_outline_add_24px,
            )

        fun getNoModificationDateFilter() =
            FilterImpl(
                title = "No modify time",
                sql = QueryTemplate().where(Task.MODIFICATION_DATE.eq(0)).toString(),
                icon = R.drawable.ic_outline_edit_24px,
            )

        fun getRecentlyModifiedFilter(r: Resources) =
            RecentlyModifiedFilter(r.getString(R.string.BFE_Recent))

        fun getSnoozedFilter(r: Resources) = SnoozedFilter(r.getString(R.string.filter_snoozed))

        fun getTimerFilter(r: Resources) = TimerFilter(r.getString(R.string.TFE_workingOn))

        fun getNotificationsFilter(context: Context) = NotificationsFilter(context.getString(R.string.notifications))

        @JvmStatic
        fun isInbox(context: Context, filter: Filter?) =
                filter == getMyTasksFilter(context.resources)

        @JvmStatic
        fun isTodayFilter(context: Context, filter: Filter?) =
                filter == getTodayFilter(context.resources)

        fun isRecentlyModifiedFilter(context: Context, filter: Filter?) =
                filter == getRecentlyModifiedFilter(context.resources)

        fun isSnoozedFilter(context: Context, filter: Filter?) =
                filter == getSnoozedFilter(context.resources)

        fun isNotificationsFilter(context: Context, filter: Filter?) =
            filter == getNotificationsFilter(context)
    }
}