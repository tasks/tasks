/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.example.astrid.filter;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.Order;
import com.todoroo.andlib.QueryTemplate;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.data.Task;

/**
 * Exposes Filters when requested
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class FilterExposer extends BroadcastReceiver {

    @SuppressWarnings("nls")
    @Override
    public void onReceive(Context context, Intent intent) {
        FilterListItem[] list = new FilterListItem[2];

        list[0] = new FilterListHeader("Sample Filters");

        ContentValues completedValues = new ContentValues();
        completedValues.put(Task.COMPLETION_DATE.name, Filter.VALUE_NOW);
        list[1] = new Filter("Completed by Alpha",
                "Completed by Alpha",
                new QueryTemplate().where(Task.COMPLETION_DATE.gt(0)).orderBy(Order.asc(Task.TITLE)),
                completedValues);

        // transmit filter list
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
