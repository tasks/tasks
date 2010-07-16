/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.rmilk.Utilities.ListContainer;
import com.todoroo.astrid.rmilk.data.MilkDataService;
import com.todoroo.astrid.rmilk.data.MilkTask;

/**
 * Exposes filters based on RTM lists
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MilkFilterExposer extends BroadcastReceiver {

    @SuppressWarnings("nls")
    private Filter filterFromList(Context context, ListContainer list) {
        String listTitle = context.getString(R.string.rmilk_FEx_list_item).
            replace("$N", list.name).replace("$C", Integer.toString(list.count));
        String title = context.getString(R.string.rmilk_FEx_list_title, list.name);
        ContentValues values = new ContentValues();
        values.put(Metadata.KEY.name, MilkTask.METADATA_KEY);
        values.put(MilkTask.LIST_ID.name, list.id);
        Filter filter = new Filter(listTitle, title, new QueryTemplate().join(
                MilkDataService.METADATA_JOIN).where(Criterion.and(
                        MetadataCriteria.withKey(MilkTask.METADATA_KEY),
                        TaskCriteria.isActive(),
                        TaskCriteria.isVisible(DateUtilities.now()),
                        MilkTask.LIST_ID.eq(list.id))),
                values);

        return filter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // if we aren't logged in, don't expose features
        if(!Utilities.isLoggedIn())
            return;

        ListContainer[] lists = MilkDataService.getInstance().getListsWithCounts();

        // If user does not have any tags, don't show this section at all
        if(lists.length == 0)
            return;

        Filter[] listFilters = new Filter[lists.length];
        for(int i = 0; i < lists.length; i++)
            listFilters[i] = filterFromList(context, lists[i]);

        FilterListHeader rtmHeader = new FilterListHeader(context.getString(R.string.rmilk_FEx_header));
        FilterCategory rtmLists = new FilterCategory(context.getString(R.string.rmilk_FEx_list),
                listFilters);

        // transmit filter list
        FilterListItem[] list = new FilterListItem[2];
        list[0] = rtmHeader;
        list[1] = rtmLists;
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, Utilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
