/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core

import org.tasks.R
import org.tasks.data.dao.TaskDao
import org.tasks.filters.Filter
import org.tasks.filters.NotificationsFilter
import org.tasks.filters.RecentlyModifiedFilter
import org.tasks.filters.SnoozedFilter
import org.tasks.filters.TimerFilter
import org.tasks.filters.TodayFilter
import org.tasks.preferences.Preferences
import javax.inject.Inject

class BuiltInFilterExposer @Inject constructor(
        private val preferences: Preferences,
        private val taskDao: TaskDao
) {
    suspend fun filters(): List<Filter> {
        val filters: MutableList<Filter> = ArrayList()
        if (preferences.getBoolean(R.string.p_show_today_filter, true)) {
            filters.add(TodayFilter.create())
        }
        if (preferences.getBoolean(R.string.p_show_recently_modified_filter, true)) {
            filters.add(RecentlyModifiedFilter.create())
        }
        if (taskDao.snoozedReminders() > 0) {
            filters.add(SnoozedFilter.create())
        }
        if (taskDao.activeTimers() > 0) {
            filters.add(TimerFilter.create())
        }
        if (taskDao.hasNotifications() > 0) {
            filters.add(NotificationsFilter.create())
        }
        return filters
    }
}
