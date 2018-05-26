/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;

import android.content.Context;
import android.content.res.Resources;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ForApplication;

/**
 * Exposes "working on" filter to the NavigationDrawerFragment
 *
 * @author Tim Su <tim@todoroo.com>
 */
public final class TimerFilterExposer {

  private final TaskDao taskDao;
  private final Context context;

  @Inject
  public TimerFilterExposer(@ForApplication Context context, TaskDao taskDao) {
    this.context = context;
    this.taskDao = taskDao;
  }

  public static Filter createFilter(Context context) {
    Resources r = context.getResources();
    Filter filter =
        new Filter(
            r.getString(R.string.TFE_workingOn),
            new QueryTemplate()
                .where(Criterion.and(Task.TIMER_START.gt(0), Task.DELETION_DATE.eq(0))));
    filter.icon = R.drawable.ic_timer_24dp;
    return filter;
  }

  public List<Filter> getFilters() {
    if (taskDao.activeTimers() == 0) {
      return emptyList();
    }

    return newArrayList(createFilter(context));
  }
}
