/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;

import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;

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

    private final TaskService taskService;
    private final Context context;

    @Inject
    public TimerFilterExposer(@ForApplication Context context, TaskService taskService) {
        this.context = context;
        this.taskService = taskService;
    }

    public List<Filter> getFilters() {
        if(taskService.count(Query.select(Task.ID).where(Task.TIMER_START.gt(0))) == 0) {
            return emptyList();
        }

        return newArrayList(createFilter(context));
    }

    public static Filter createFilter(Context context) {
        Resources r = context.getResources();
        ContentValues values = new ContentValues();
        values.put(Task.TIMER_START.name, Filter.VALUE_NOW);
        return new Filter(r.getString(R.string.TFE_workingOn),
                new QueryTemplate().where(Task.TIMER_START.gt(0)),
                values);
    }
}
