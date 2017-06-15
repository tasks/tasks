/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import android.content.Context;
import android.content.res.Resources;

import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.injection.ForApplication;

import java.util.List;

import javax.inject.Inject;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;

/**
 * Exposes "working on" filter to the NavigationDrawerFragment
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TimerFilterExposer {

    private final TaskDao taskDao;
    private final Context context;

    @Inject
    public TimerFilterExposer(@ForApplication Context context, TaskDao taskDao) {
        this.context = context;
        this.taskDao = taskDao;
    }

    public List<Filter> getFilters() {
        if(taskDao.count(Query.select(Task.ID).where(Task.TIMER_START.gt(0))) == 0) {
            return emptyList();
        }

        return newArrayList(createFilter(context));
    }

    public static Filter createFilter(Context context) {
        Resources r = context.getResources();
        Filter filter = new Filter(r.getString(R.string.TFE_workingOn), new QueryTemplate().where(Task.TIMER_START.gt(0)));
        filter.icon = R.drawable.ic_timer_24dp;
        return filter;
    }
}
