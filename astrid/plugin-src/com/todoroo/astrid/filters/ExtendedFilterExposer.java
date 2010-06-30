/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.filters;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.FilterListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListHeader;
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

    @Override
    public void onReceive(Context context, Intent intent) {
        // build filters
        FilterListHeader header = new FilterListHeader(ExtendedPlugin.pluginIdentifier,
                "Extended");

        Filter alphabetical = new Filter(ExtendedPlugin.pluginIdentifier,
                "Inbox (sorted by name)",
                "Inbox (sorted by name)",
                new QueryTemplate().where(Criterion.and(TaskCriteria.isActive(),
                        TaskCriteria.isVisible(DateUtilities.now()))).
                        orderBy(Order.asc(Task.TITLE)),
                null);

        ContentValues hiddenValues = new ContentValues();
        hiddenValues.put(Task.HIDE_UNTIL.name, DateUtilities.now() + DateUtilities.ONE_DAY);
        Filter hidden = new Filter(ExtendedPlugin.pluginIdentifier,
                "Hidden Tasks",
                "Hidden Tasks",
                new QueryTemplate().where(Criterion.and(TaskCriteria.isActive(),
                        Criterion.not(TaskCriteria.isVisible(DateUtilities.now())))).
                        orderBy(Order.asc(Task.HIDE_UNTIL)),
                hiddenValues);

        Filter deleted = new Filter(ExtendedPlugin.pluginIdentifier,
                "Deleted Tasks",
                "Deleted Tasks",
                new QueryTemplate().where(TaskCriteria.isDeleted()).
                        orderBy(Order.desc(Task.DELETION_DATE)),
                null);

        // transmit filter list
        FilterListItem[] list = new FilterListItem[4];
        list[0] = header;
        list[1] = alphabetical;
        list[2] = hidden;
        list[3] = deleted;
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ITEMS, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
