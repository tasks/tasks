/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.MetadataApiDao.MetadataCriteria;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao.TaskCriteria;

/**
 * Exposes filters based on lists
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksFilterExposer extends BroadcastReceiver {

    @Autowired private GtasksListService gtasksListService;
    @Autowired private GtasksPreferenceService gtasksPreferenceService;

    private StoreObject[] lists;

    public static Filter filterFromList(Context context, StoreObject list) {
        String listName = list.getValue(GtasksList.NAME);
        ContentValues values = new ContentValues();
        values.putAll(GtasksMetadata.createEmptyMetadata(AbstractModel.NO_ID).getMergedValues());
        values.remove(Metadata.TASK.name);
        values.put(GtasksMetadata.LIST_ID.name, list.getValue(GtasksList.REMOTE_ID));
        FilterWithCustomIntent filter = new FilterWithCustomIntent(listName,
                ContextManager.getString(R.string.gtasks_FEx_title, listName), new QueryTemplate().join(
                Join.left(Metadata.TABLE, Task.ID.eq(Metadata.TASK))).where(Criterion.and(
                        MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY),
                        TaskCriteria.notDeleted(),
                        GtasksMetadata.LIST_ID.eq(list.getValue(GtasksList.REMOTE_ID)))).orderBy(
                                Order.asc(Functions.cast(GtasksMetadata.ORDER, "INTEGER"))), //$NON-NLS-1$
                values);
        filter.listingIcon = ((BitmapDrawable)context.getResources().getDrawable(R.drawable.gtasks_icon)).getBitmap();
        filter.customTaskList = new ComponentName(ContextManager.getContext(), GtasksListActivity.class);

        return filter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        DependencyInjectionService.getInstance().inject(this);

        // if we aren't logged in, don't expose features
        if(!gtasksPreferenceService.isLoggedIn())
            return;

        lists = gtasksListService.getLists();

        // If user does not have any lists, don't show this section at all
        if(noListsToShow())
            return;

        Filter[] listFilters = new Filter[lists.length];
        for(int i = 0; i < lists.length; i++)
            listFilters[i] = filterFromList(context, lists[i]);

        FilterListHeader header = new FilterListHeader(context.getString(R.string.gtasks_FEx_header));
        FilterCategory listsCategory = new FilterCategory(context.getString(R.string.gtasks_FEx_list),
                listFilters);

        // transmit filter list
        FilterListItem[] list = new FilterListItem[2];
        list[0] = header;
        list[1] = listsCategory;
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, GtasksPreferenceService.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    private boolean noListsToShow() {
        return lists.length == 0;
    }

}
