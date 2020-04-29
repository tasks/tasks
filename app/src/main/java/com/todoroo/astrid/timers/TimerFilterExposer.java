/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.timers;

import android.content.Context;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.jetbrains.annotations.Nullable;
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

  static Filter createFilter(Context context) {
    Filter filter =
        new Filter(
            context.getString(R.string.TFE_workingOn),
            new QueryTemplate()
                .where(Criterion.and(Task.TIMER_START.gt(0), Task.DELETION_DATE.eq(0))));
    filter.icon = R.drawable.ic_outline_timer_24px;
    return filter;
  }

  public @Nullable Filter getFilters() {
    return taskDao.activeTimers() == 0 ? null : createFilter(context);
  }
}
