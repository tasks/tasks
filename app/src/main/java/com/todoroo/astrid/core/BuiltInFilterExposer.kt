/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core

import android.content.Context
import android.content.res.Resources
import com.todoroo.andlib.sql.Criterion.Companion.and
import com.todoroo.andlib.sql.Criterion.Companion.or
import com.todoroo.andlib.sql.Join
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.timers.TimerPlugin
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.data.*
import org.tasks.data.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.filters.NotificationsFilter
import org.tasks.filters.RecentlyModifiedFilter
import org.tasks.filters.SnoozedFilter
import org.tasks.filters.SortableFilter
import org.tasks.preferences.Preferences
import org.tasks.themes.CustomIcons
import javax.inject.Inject

/**
 * Exposes Astrid's built in filters to the NavigationDrawerFragment
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
class BuiltInFilterExposer @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val preferences: Preferences,
        private val taskDao: TaskDao) {

    val myTasksFilter: Filter
        get() {
            val myTasksFilter = getMyTasksFilter(context.resources)
            myTasksFilter.icon = CustomIcons.ALL_INBOX
            return myTasksFilter
        }

    suspend fun filters(): List<Filter> {
        val r = context.resources
        val filters: MutableList<Filter> = ArrayList()
        if (preferences.getBoolean(R.string.p_show_today_filter, true)) {
            val todayFilter = getTodayFilter(r)
            todayFilter.icon = CustomIcons.TODAY
            filters.add(todayFilter)
        }
        if (preferences.getBoolean(R.string.p_show_recently_modified_filter, true)) {
            val recentlyModifiedFilter = getRecentlyModifiedFilter(r)
            recentlyModifiedFilter.icon = CustomIcons.HISTORY
            filters.add(recentlyModifiedFilter)
        }
        if (taskDao.snoozedReminders() > 0) {
            val snoozedFilter = getSnoozedFilter(r)
            snoozedFilter.icon = R.drawable.ic_snooze_white_24dp
            filters.add(snoozedFilter)
        }
        if (taskDao.activeTimers() > 0) {
            filters.add(TimerPlugin.createFilter(context))
        }
        if (taskDao.hasNotifications() > 0) {
            val element = NotificationsFilter(context)
            element.icon = R.drawable.ic_outline_notifications_24px
            filters.add(element)
        }
        return filters
    }

    companion object {
        /** Build inbox filter  */
        fun getMyTasksFilter(r: Resources): Filter {
            return SortableFilter(
                    r.getString(R.string.BFE_Active),
                    QueryTemplate().where(activeAndVisible()))
        }

        fun getTodayFilter(r: Resources): Filter {
            val todayTitle = AndroidUtilities.capitalize(r.getString(R.string.today))
            val todayValues: MutableMap<String?, Any> = HashMap()
            todayValues[Task.DUE_DATE.name] = PermaSql.VALUE_NOON
            return SortableFilter(
                    todayTitle,
                    QueryTemplate()
                            .where(
                                    and(
                                            activeAndVisible(),
                                            Task.DUE_DATE.gt(0),
                                            Task.DUE_DATE.lte(PermaSql.VALUE_EOD))),
                    todayValues)
        }

        fun getNoListFilter() =
                Filter(
                        "No list",
                        QueryTemplate()
                                .join(Join.left(GoogleTask.TABLE, GoogleTask.TASK.eq(Task.ID)))
                                .join(Join.left(CaldavTask.TABLE, CaldavTask.TASK.eq(Task.ID)))
                                .where(and(GoogleTask.ID.eq(null), CaldavTask.ID.eq(null)))
                ).apply {
                    icon = R.drawable.ic_outline_cloud_off_24px
                }

        fun getDeleted() =
                Filter("Deleted", QueryTemplate().where(Task.DELETION_DATE.gt(0)))
                        .apply { icon = R.drawable.ic_outline_delete_24px }

        fun getMissingListFilter() =
                Filter(
                        "Missing list",
                        QueryTemplate()
                                .join(Join.left(GoogleTask.TABLE, GoogleTask.TASK.eq(Task.ID)))
                                .join(Join.left(CaldavTask.TABLE, CaldavTask.TASK.eq(Task.ID)))
                                .join(Join.left(GoogleTaskList.TABLE, GoogleTaskList.REMOTE_ID.eq(GoogleTask.LIST)))
                                .join(Join.left(CaldavCalendar.TABLE, CaldavCalendar.UUID.eq(CaldavTask.CALENDAR)))
                                .where(or(
                                        and(GoogleTask.ID.gt(0), GoogleTaskList.REMOTE_ID.eq(null)),
                                        and(CaldavTask.ID.gt(0), CaldavCalendar.UUID.eq(null))))
                ).apply {
                    icon = R.drawable.ic_outline_cloud_off_24px
                }

        fun getMissingAccountFilter() =
                Filter(
                        "Missing account",
                        QueryTemplate()
                                .join(Join.left(GoogleTask.TABLE, and(GoogleTask.TASK.eq(Task.ID))))
                                .join(Join.left(CaldavTask.TABLE, and(CaldavTask.TASK.eq(Task.ID))))
                                .join(Join.left(GoogleTaskList.TABLE, GoogleTaskList.REMOTE_ID.eq(GoogleTask.LIST)))
                                .join(Join.left(CaldavCalendar.TABLE, CaldavCalendar.UUID.eq(CaldavTask.CALENDAR)))
                                .join(Join.left(GoogleTaskAccount.TABLE, GoogleTaskAccount.ACCOUNT.eq(GoogleTaskList.ACCOUNT)))
                                .join(Join.left(CaldavAccount.TABLE, CaldavAccount.UUID.eq(CaldavCalendar.ACCOUNT)))
                                .where(or(
                                        and(GoogleTask.ID.gt(0), GoogleTaskAccount.ACCOUNT.eq(null)),
                                        and(CaldavTask.ID.gt(0), CaldavAccount.UUID.eq(null))))
                ).apply {
                    icon = R.drawable.ic_outline_cloud_off_24px
                }

        fun getNoTitleFilter() =
                Filter(
                        "No title",
                        QueryTemplate().where(or(Task.TITLE.eq(null), Task.TITLE.eq("")))
                ).apply {
                    icon = R.drawable.ic_outline_clear_24px
                }

        fun getNoCreateDateFilter() =
                Filter("No create time", QueryTemplate().where(Task.CREATION_DATE.eq(0)))
                        .apply { icon = R.drawable.ic_outline_add_24px }

        fun getNoModificationDateFilter() =
                Filter("No modify time", QueryTemplate().where(Task.MODIFICATION_DATE.eq(0)))
                        .apply { icon = R.drawable.ic_outline_edit_24px }

        fun getRecentlyModifiedFilter(r: Resources) =
                RecentlyModifiedFilter(r.getString(R.string.BFE_Recent))

        fun getSnoozedFilter(r: Resources) = SnoozedFilter(r.getString(R.string.filter_snoozed))

        fun getNotificationsFilter(context: Context) = NotificationsFilter(context)

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