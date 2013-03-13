/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

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
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategoryWithNewButton;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;

/**
 * Exposes filters based on lists
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksFilterExposer extends BroadcastReceiver implements AstridFilterExposer {

    @Autowired private GtasksListService gtasksListService;
    @Autowired private GtasksPreferenceService gtasksPreferenceService;
    @Autowired private ActFmPreferenceService actFmPreferenceService;

    static {
        AstridDependencyInjector.initialize();
    }

    private StoreObject[] lists;

    public static Filter filterFromList(Context context, StoreObject list) {
        String listName = list.getValue(GtasksList.NAME);
        ContentValues values = new ContentValues();
        values.putAll(GtasksMetadata.createEmptyMetadata(AbstractModel.NO_ID).getMergedValues());
        values.remove(Metadata.TASK.name);
        values.put(GtasksMetadata.LIST_ID.name, list.getValue(GtasksList.REMOTE_ID));
        values.put(GtasksMetadata.ORDER.name, PermaSql.VALUE_NOW);
        FilterWithCustomIntent filter = new FilterWithCustomIntent(listName,
                ContextManager.getString(R.string.gtasks_FEx_title, listName), new QueryTemplate().join(
                Join.left(Metadata.TABLE, Task.ID.eq(Metadata.TASK))).where(Criterion.and(
                        MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY),
                        TaskCriteria.notDeleted(),
                        GtasksMetadata.LIST_ID.eq(list.getValue(GtasksList.REMOTE_ID)))).orderBy(
                                Order.asc(Functions.cast(GtasksMetadata.ORDER, "LONG"))), //$NON-NLS-1$
                values);
        filter.listingIcon = ((BitmapDrawable)context.getResources().getDrawable(R.drawable.gtasks_icon)).getBitmap();
        filter.customTaskList = new ComponentName(ContextManager.getContext(), GtasksListFragment.class);
        Bundle extras = new Bundle();
        extras.putLong(GtasksListFragment.TOKEN_STORE_ID, list.getId());
        filter.customExtras = extras;

        return filter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        FilterListItem[] list = prepareFilters(context);
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, GtasksPreferenceService.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    private FilterListItem[] prepareFilters(Context context) {
        DependencyInjectionService.getInstance().inject(this);

        // if we aren't logged in (or we are logged in to astrid.com), don't expose features
        if(!gtasksPreferenceService.isLoggedIn() || actFmPreferenceService.isLoggedIn())
            return null;

        lists = gtasksListService.getLists();

        // If user does not have any lists, don't show this section at all
        if(noListsToShow())
            return null;

        Filter[] listFilters = new Filter[lists.length];
        for(int i = 0; i < lists.length; i++)
            listFilters[i] = filterFromList(context, lists[i]);

        FilterCategoryWithNewButton listsCategory = new FilterCategoryWithNewButton(context.getString(R.string.gtasks_FEx_header),
                listFilters);
        listsCategory.label = context.getString(R.string.tag_FEx_add_new);
        listsCategory.intent = PendingIntent.getActivity(context, 0, new Intent(context, GtasksListAdder.class), 0);

        // transmit filter list
        FilterListItem[] list = new FilterListItem[1];
        list[0] = listsCategory;
        return list;
    }

    private boolean noListsToShow() {
        return lists.length == 0;
    }

    @Override
    public FilterListItem[] getFilters() {
        if (ContextManager.getContext() == null)
            return null;

        return prepareFilters(ContextManager.getContext());
    }

}
