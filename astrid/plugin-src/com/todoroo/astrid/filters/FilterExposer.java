/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.filters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.FilterListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.model.Task;

/**
 * Exposes Astrid's built in filters to the {@link FilterListActivity}
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class FilterExposer extends BroadcastReceiver {

    public static Filter buildInboxFilter(Resources r) {
        return new Filter(r.getString(R.string.BFE_Inbox),
                r.getString(R.string.BFE_Inbox),
                /*String.format("WHERE %s AND %s ORDER BY CASE %s WHEN 0 THEN (%d + 1000 * %s) ELSE (%s + 1000 * %s) END ASC", //$NON-NLS-1$
                        TaskSql.isActive(), TaskSql.isVisible(DateUtilities.now()),
                        Task.DUE_DATE, DateUtilities.now() + 60 * 24 * 3600, Task.IMPORTANCE,
                            Task.DUE_DATE, Task.IMPORTANCE)*/ "",
                null);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Resources r = context.getResources();

        // build filters
        Filter inbox = buildInboxFilter(r);

        Filter all = new Filter(r.getString(R.string.BFE_All),
                r.getString(R.string.BFE_All),
                String.format("ORDER BY %s DESC", //$NON-NLS-1$
                        Task.ID.name),
                null);

        // transmit filter list
        FilterListItem[] list = new FilterListItem[2];
        list[0] = inbox;
        list[1] = all;
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ITEMS, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
