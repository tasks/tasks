/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.R;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.rmilk.Utilities.ListContainer;
import com.todoroo.astrid.rmilk.data.MilkDataService;

/**
 * Exposes filters based on RTM lists
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class FilterExposer extends BroadcastReceiver {


    @SuppressWarnings("nls")
    private Filter filterFromList(Context context, ListContainer list) {
        String listTitle = context.getString(R.string.rmilk_FEx_list_item).
            replace("$N", list.name).replace("$C", Integer.toString(list.count));
        String title = context.getString(R.string.rmilk_FEx_list_title, list.name);
        Filter filter = new Filter(listTitle, title,
                    "TODO",
                    "TODO");

        return filter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // if we aren't logged in, don't expose features
        if(!Utilities.isLoggedIn())
            return;

        MilkDataService service = new MilkDataService(context);
        ListContainer[] lists = service.getListsWithCounts();

        // If user does not have any tags, don't show this section at all
        if(lists.length == 0)
            return;

        Filter[] listFilters = new Filter[lists.length];
        for(int i = 0; i < lists.length; i++)
            listFilters[i] = filterFromList(context, lists[i]);

        FilterListHeader rtmHeader = new FilterListHeader(context.getString(R.string.rmilk_FEx_header));
        FilterCategory rtmLists = new FilterCategory(
                context.getString(R.string.rmilk_FEx_list), listFilters);

        // transmit filter list
        FilterListItem[] list = new FilterListItem[2];
        list[0] = rtmHeader;
        list[1] = rtmLists;
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ITEMS, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
