/**
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.rmilk;

import org.weloveastrid.rmilk.data.MilkListFields;
import org.weloveastrid.rmilk.data.MilkListService;
import org.weloveastrid.rmilk.data.MilkTaskFields;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.MetadataApiDao.MetadataCriteria;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao.TaskCriteria;

/**
 * Exposes filters based on RTM lists
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MilkFilterExposer extends BroadcastReceiver implements AstridFilterExposer {

    @Autowired private MilkListService milkListService;

    static {
        MilkDependencyInjector.initialize();
    }

    private Filter filterFromList(Context context, StoreObject list) {
        String listName = list.getValue(MilkListFields.NAME);
        String title = context.getString(R.string.rmilk_FEx_list_title,
                listName);
        ContentValues values = new ContentValues();
        values.put(Metadata.KEY.name, MilkTaskFields.METADATA_KEY);
        values.put(MilkTaskFields.LIST_ID.name, list.getValue(MilkListFields.REMOTE_ID));
        values.put(MilkTaskFields.TASK_SERIES_ID.name, 0);
        values.put(MilkTaskFields.TASK_ID.name, 0);
        values.put(MilkTaskFields.REPEATING.name, 0);
        Filter filter = new Filter(listName, title, new QueryTemplate().join(
                Join.left(Metadata.TABLE, Task.ID.eq(Metadata.TASK))).where(Criterion.and(
                        MetadataCriteria.withKey(MilkTaskFields.METADATA_KEY),
                        TaskCriteria.activeAndVisible(),
                        MilkTaskFields.LIST_ID.eq(list.getValue(MilkListFields.REMOTE_ID)))),
                values);

        return filter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Resources r = context.getResources();
        ContextManager.setContext(context);

        FilterListItem[] list = prepareFilters(r);
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, MilkUtilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    private FilterListItem[] prepareFilters(Resources r) {
        // if we aren't logged in, don't expose features
        if(!MilkUtilities.INSTANCE.isLoggedIn())
            return null;

        DependencyInjectionService.getInstance().inject(this);

        StoreObject[] lists = milkListService.getLists();

        // If user does not have any tags, don't show this section at all
        if(lists.length == 0)
            return null;

        Filter[] listFilters = new Filter[lists.length];
        for(int i = 0; i < lists.length; i++)
            listFilters[i] = filterFromList(ContextManager.getContext(), lists[i]);

        FilterListHeader rtmHeader = new FilterListHeader(ContextManager.getContext().getString(R.string.rmilk_FEx_header));
        FilterCategory rtmLists = new FilterCategory(ContextManager.getContext().getString(R.string.rmilk_FEx_list),
                listFilters);

        // transmit filter list
        FilterListItem[] list = new FilterListItem[2];
        list[0] = rtmHeader;
        list[1] = rtmLists;

        return list;
    }

    @Override
    public FilterListItem[] getFilters() {
        if (ContextManager.getContext() == null || ContextManager.getContext().getResources() == null)
            return null;

        Resources r = ContextManager.getContext().getResources();
        return prepareFilters(r);
    }

}
