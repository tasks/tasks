/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core

import android.content.Context
import android.content.res.Resources
import com.todoroo.andlib.sql.Criterion.Companion.and
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.timers.TimerPlugin
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.data.TaskDao
import org.tasks.data.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.filters.RecentlyModifiedFilter
import org.tasks.filters.SortableFilter
import org.tasks.preferences.Preferences
import org.tasks.themes.CustomIcons
import java.util.*
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
        if (taskDao.activeTimers() > 0) {
            filters.add(TimerPlugin.createFilter(context))
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

        fun getRecentlyModifiedFilter(r: Resources) =
                RecentlyModifiedFilter(r.getString(R.string.BFE_Recent))

        @JvmStatic
        fun isInbox(context: Context, filter: Filter?) =
                filter != null && filter == getMyTasksFilter(context.resources)

        @JvmStatic
        fun isTodayFilter(context: Context, filter: Filter?) =
                filter != null && filter == getTodayFilter(context.resources)

        fun isRecentlyModifiedFilter(context: Context, filter: Filter?) =
                filter != null && filter == getRecentlyModifiedFilter(context.resources)
    }
}