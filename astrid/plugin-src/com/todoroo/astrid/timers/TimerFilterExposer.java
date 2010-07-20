/**
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
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.activity.FilterListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.model.Task;

/**
 * Exposes "working on" filter to the {@link FilterListActivity}
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TimerFilterExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if(PluginServices.getTaskService().count(Query.select(Task.ID).
                where(Task.TIMER_START.gt(0))) == 0)
            return;

        Filter workingOn = createFilter(context);

        // transmit filter list
        FilterListItem[] list = new FilterListItem[1];
        list[0] = workingOn;
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
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

}
