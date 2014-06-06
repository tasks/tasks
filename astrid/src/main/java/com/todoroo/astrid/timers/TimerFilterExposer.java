/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.activity.FilterListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.injection.Injector;

import javax.inject.Inject;

/**
 * Exposes "working on" filter to the {@link FilterListFragment}
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TimerFilterExposer extends InjectingBroadcastReceiver implements AstridFilterExposer {

    @Inject TaskService taskService;
    @Inject @ForApplication Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        ContextManager.setContext(context);
        FilterListItem[] list = prepareFilters();

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    private FilterListItem[] prepareFilters() {
        if(taskService.count(Query.select(Task.ID).
                where(Task.TIMER_START.gt(0))) == 0) {
            return null;
        }

        Filter workingOn = createFilter(context);

        // transmit filter list
        FilterListItem[] list = new FilterListItem[1];
        list[0] = workingOn;
        return list;
    }

    public static Filter createFilter(Context context) {
        Resources r = context.getResources();
        ContentValues values = new ContentValues();
        values.put(Task.TIMER_START.name, Filter.VALUE_NOW);
        Filter workingOn = new Filter(r.getString(R.string.TFE_workingOn),
                r.getString(R.string.TFE_workingOn),
                new QueryTemplate().where(Task.TIMER_START.gt(0)),
                values);
        workingOn.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.tango_clock)).getBitmap();
        return workingOn;
    }

    @Override
    public FilterListItem[] getFilters(Injector injector) {
        injector.inject(this);

        return prepareFilters();
    }
}
