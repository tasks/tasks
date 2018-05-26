/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.Context;
import android.content.res.Resources;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.Tag;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

/**
 * Exposes Astrid's built in filters to the NavigationDrawerFragment
 *
 * @author Tim Su <tim@todoroo.com>
 */
public final class BuiltInFilterExposer {

  private final Preferences preferences;
  private final Context context;

  @Inject
  public BuiltInFilterExposer(@ForApplication Context context, Preferences preferences) {
    this.context = context;
    this.preferences = preferences;
  }

  /** Build inbox filter */
  public static Filter getMyTasksFilter(Resources r) {
    return new Filter(
        r.getString(R.string.BFE_Active),
        new QueryTemplate().where(TaskCriteria.activeAndVisible()));
  }

  public static Filter getTodayFilter(Resources r) {
    String todayTitle = AndroidUtilities.capitalize(r.getString(R.string.today));
    Map<String, Object> todayValues = new HashMap<>();
    todayValues.put(Task.DUE_DATE.name, PermaSql.VALUE_NOON);
    return new Filter(
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
    return new Filter(
        r.getString(R.string.BFE_Recent),
        new QueryTemplate()
            .where(TaskCriteria.notDeleted())
            .orderBy(Order.desc(Task.MODIFICATION_DATE))
            .limit(15));
  }

  public static Filter getUncategorizedFilter(Resources r) {
    return new Filter(
        r.getString(R.string.tag_FEx_untagged),
        new QueryTemplate()
            .where(
                Criterion.and(
                    Criterion.not(
                        Task.UUID.in(Query.select(Field.field("task_uid")).from(Tag.TABLE))),
                    TaskCriteria.isActive(),
                    TaskCriteria.isVisible())));
  }

  public static boolean isInbox(Context context, Filter filter) {
    return filter != null && filter.equals(getMyTasksFilter(context.getResources()));
  }

  public static boolean isTodayFilter(Context context, Filter filter) {
    return filter != null && filter.equals(getTodayFilter(context.getResources()));
  }

  public static boolean isUncategorizedFilter(Context context, Filter filter) {
    return filter != null && filter.equals(getUncategorizedFilter(context.getResources()));
  }

  public static boolean isRecentlyModifiedFilter(Context context, Filter filter) {
    return filter != null && filter.equals(getRecentlyModifiedFilter(context.getResources()));
  }

  public Filter getMyTasksFilter() {
    Filter myTasksFilter = getMyTasksFilter(context.getResources());
    myTasksFilter.icon = R.drawable.ic_inbox_24dp;
    return myTasksFilter;
  }

  public List<Filter> getFilters() {
    Resources r = context.getResources();
    // core filters
    List<Filter> filters = new ArrayList<>();

    if (preferences.getBoolean(R.string.p_show_today_filter, true)) {
      Filter todayFilter = getTodayFilter(r);
      todayFilter.icon = R.drawable.ic_today_24dp;
      filters.add(todayFilter);
    }
    if (preferences.getBoolean(R.string.p_show_recently_modified_filter, true)) {
      Filter recentlyModifiedFilter = getRecentlyModifiedFilter(r);
      recentlyModifiedFilter.icon = R.drawable.ic_history_24dp;
      filters.add(recentlyModifiedFilter);
    }
    if (preferences.getBoolean(R.string.p_show_not_in_list_filter, true)) {
      Filter uncategorizedFilter = getUncategorizedFilter(r);
      uncategorizedFilter.icon = R.drawable.ic_label_outline_24dp;
      filters.add(uncategorizedFilter);
    }
    // transmit filter list
    return filters;
  }
}
