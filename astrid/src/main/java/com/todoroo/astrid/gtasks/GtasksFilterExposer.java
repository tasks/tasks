/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.injection.Injector;

import java.util.List;

import javax.inject.Inject;

/**
 * Exposes filters based on lists
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksFilterExposer extends InjectingBroadcastReceiver implements AstridFilterExposer {

    @Inject GtasksListService gtasksListService;
    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject @ForApplication Context context;
    @Inject GtasksMetadata gtasksMetadata;

    private List<StoreObject> lists;

    public static Filter filterFromList(GtasksMetadata gtasksMetadata, Context context, StoreObject list) {
        String listName = list.getValue(GtasksList.NAME);
        ContentValues values = new ContentValues();
        values.putAll(gtasksMetadata.createEmptyMetadata(AbstractModel.NO_ID).getMergedValues());
        values.remove(Metadata.TASK.name);
        values.put(GtasksMetadata.LIST_ID.name, list.getValue(GtasksList.REMOTE_ID));
        values.put(GtasksMetadata.ORDER.name, PermaSql.VALUE_NOW);
        FilterWithCustomIntent filter = new FilterWithCustomIntent(listName,
                context.getString(R.string.gtasks_FEx_title, listName), new QueryTemplate().join(
                Join.left(Metadata.TABLE, Task.ID.eq(Metadata.TASK))).where(Criterion.and(
                        MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY),
                        TaskCriteria.notDeleted(),
                        GtasksMetadata.LIST_ID.eq(list.getValue(GtasksList.REMOTE_ID)))).orderBy(
                                Order.asc(Functions.cast(GtasksMetadata.ORDER, "LONG"))), //$NON-NLS-1$
                values);
        filter.customTaskList = new ComponentName(context, GtasksListFragment.class);
        Bundle extras = new Bundle();
        extras.putLong(GtasksListFragment.TOKEN_STORE_ID, list.getId());
        filter.customExtras = extras;

        return filter;
    }

    private FilterListItem[] prepareFilters() {
        // if we aren't logged in (or we are logged in to astrid.com), don't expose features
        if(!gtasksPreferenceService.isLoggedIn()) {
            return null;
        }

        lists = gtasksListService.getLists();

        // If user does not have any lists, don't show this section at all
        if(noListsToShow()) {
            return null;
        }

        Filter[] listFilters = new Filter[lists.size()];
        for(int i = 0; i < lists.size(); i++) {
            listFilters[i] = filterFromList(gtasksMetadata, context, lists.get(i));
        }

        return listFilters;
    }

    private boolean noListsToShow() {
        return lists.isEmpty();
    }

    @Override
    public FilterListItem[] getFilters(Injector injector) {
        injector.inject(this);

        return prepareFilters();
    }
}
