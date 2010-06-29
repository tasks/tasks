/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.filters;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.FilterListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Task;

/**
 * Exposes Astrid's built in filters to the {@link FilterListActivity}
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class ExtendedFilterExposer extends BroadcastReceiver {

    @SuppressWarnings("nls")
    @Override
    public void onReceive(Context context, Intent intent) {
        Resources r = context.getResources();

        // build filters
        ContentValues hiddenValues = new ContentValues();
        hiddenValues.put(Task.HIDE_UNTIL, DateUtilities.now() + DateUtilities.ONE_DAY);
        Filter hidden = new Filter(ExtendedPlugin.pluginIdentifier, "Hidden Tasks",
                "Hidden Tasks",
                new QueryTemplate().where(Criterion.and(TaskCriteria.isActive(),
                        Criterion.not(TaskCriteria.isVisible(DateUtilities.now())))).
                        orderBy(Order.asc(Task.HIDE_UNTIL)),
                hiddenValues);

        Filter alphabetical = new Filter(ExtendedPlugin.pluginIdentifier,
                "Inbox (sorted by name)",
                "Inbox (sorted by name)",
                new QueryTemplate().orderBy(Order.asc(Task.TITLE)),
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
