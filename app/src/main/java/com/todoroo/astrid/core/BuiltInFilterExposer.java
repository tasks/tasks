/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.core;

import android.content.Context;
import android.content.res.Resources;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.timers.TimerPlugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.filters.RecentlyModifiedFilter;
import org.tasks.filters.SortableFilter;
import org.tasks.injection.ApplicationContext;
import org.tasks.preferences.Preferences;
import org.tasks.themes.CustomIcons;

/**
 * Exposes Astrid's built in filters to the NavigationDrawerFragment
 *
 * @author Tim Su <tim@todoroo.com>
 */
public final class BuiltInFilterExposer {

  private final Preferences preferences;
  private final TaskDao taskDao;
  private final Context context;

  @Inject
  public BuiltInFilterExposer(
      @ApplicationContext Context context, Preferences preferences, TaskDao taskDao) {
    this.context = context;
    this.preferences = preferences;
    this.taskDao = taskDao;
  }

  /** Build inbox filter */
  public static Filter getMyTasksFilter(Resources r) {
    return new SortableFilter(
        r.getString(R.string.BFE_Active),
        new QueryTemplate().where(TaskCriteria.activeAndVisible()));
  }

  public static Filter getTodayFilter(Resources r) {
    String todayTitle = AndroidUtilities.capitalize(r.getString(R.string.today));
    Map<String, Object> todayValues = new HashMap<>();
    todayValues.put(Task.DUE_DATE.name, PermaSql.VALUE_NOON);
    return new SortableFilter(
        todayTitle,
        new QueryTemplate()
            .where(
                Criterion.and(
                    TaskCriteria.activeAndVisible(),
                    Task.DUE_DATE.gt(0),
                    Task.DUE_DATE.lte(PermaSql.VALUE_EOD))),
        todayValues);
  }

  public static Filter getRecentlyModifiedFilter(Resources r) {
    return new RecentlyModifiedFilter(r.getString(R.string.BFE_Recent));
  }

  public static boolean isInbox(Context context, Filter filter) {
    return filter != null && filter.equals(getMyTasksFilter(context.getResources()));
  }

  public static boolean isTodayFilter(Context context, Filter filter) {
    return filter != null && filter.equals(getTodayFilter(context.getResources()));
  }

  public static boolean isRecentlyModifiedFilter(Context context, Filter filter) {
    return filter != null && filter.equals(getRecentlyModifiedFilter(context.getResources()));
  }

  public Filter getMyTasksFilter() {
    Filter myTasksFilter = getMyTasksFilter(context.getResources());
    myTasksFilter.icon = CustomIcons.ALL_INBOX;
    return myTasksFilter;
  }

  public List<Filter> getFilters() {
    Resources r = context.getResources();
    List<Filter> filters = new ArrayList<>();
    if (preferences.getBoolean(R.string.p_show_today_filter, true)) {
      Filter todayFilter = getTodayFilter(r);
      todayFilter.icon = CustomIcons.TODAY;
      filters.add(todayFilter);
    }
    if (preferences.getBoolean(R.string.p_show_recently_modified_filter, true)) {
      Filter recentlyModifiedFilter = getRecentlyModifiedFilter(r);
      recentlyModifiedFilter.icon = CustomIcons.HISTORY;
      filters.add(recentlyModifiedFilter);
    }
    if (taskDao.activeTimers() > 0) {
      filters.add(TimerPlugin.createFilter(context));
    }
    return filters;
  }
}
