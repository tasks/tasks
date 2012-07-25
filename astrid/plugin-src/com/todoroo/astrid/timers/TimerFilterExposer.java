/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.activity.FilterListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;

/**
 * Exposes "working on" filter to the {@link FilterListFragment}
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TimerFilterExposer extends BroadcastReceiver implements AstridFilterExposer {

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        FilterListItem[] list = prepareFilters(context);

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    private FilterListItem[] prepareFilters(Context context) {
        if(PluginServices.getTaskService().count(Query.select(Task.ID).
                where(Task.TIMER_START.gt(0))) == 0)
            return null;

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
    public FilterListItem[] getFilters() {
        if (ContextManager.getContext() == null)
            return null;

        return prepareFilters(ContextManager.getContext());
    }

}
