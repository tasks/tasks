/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.filters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;

import com.timsu.astrid.R;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
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
public final class CoreFilterExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Resources r = context.getResources();

        // build filters
        Filter inbox = buildInboxFilter(r);

        Filter all = new Filter(CorePlugin.pluginIdentifier, r.getString(R.string.BFE_All),
                r.getString(R.string.BFE_All),
                new QueryTemplate().where(Criterion.not(TaskCriteria.isDeleted())).
                        orderBy(Order.desc(Task.MODIFICATION_DATE)),
                null);
        all.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.filter_all)).getBitmap();

        // transmit filter list
        FilterListItem[] list = new FilterListItem[2];
        list[0] = inbox;
        list[1] = all;
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ITEMS, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    /**
     * Build inbox filter
     * @return
     */
    @SuppressWarnings("nls")
    public static Filter buildInboxFilter(Resources r) {
        Filter inbox = new Filter(CorePlugin.pluginIdentifier, r.getString(R.string.BFE_Inbox),
                r.getString(R.string.BFE_Inbox_title),
                new QueryTemplate().where(Criterion.and(TaskCriteria.isActive(),
                        TaskCriteria.isVisible(DateUtilities.now()))).orderBy(
                                Order.asc(Functions.caseStatement(Task.DUE_DATE.eq(0),
                                        String.format("(%d + 1000 * %s)", DateUtilities.now(), Task.IMPORTANCE),
                                        String.format("(%s + 1000 * %s)", Task.DUE_DATE, Task.IMPORTANCE)))),
                null);
        inbox.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.filter_inbox)).getBitmap();
        return inbox;
    }

}
