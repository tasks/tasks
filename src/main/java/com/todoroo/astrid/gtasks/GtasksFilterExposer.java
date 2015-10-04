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
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.injection.ForApplication;

import java.util.List;

import javax.inject.Inject;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;

/**
 * Exposes filters based on lists
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksFilterExposer {

    private final GtasksListService gtasksListService;
    private final GtasksPreferenceService gtasksPreferenceService;
    private final Context context;
    private final GtasksMetadata gtasksMetadata;

    @Inject
    public GtasksFilterExposer(@ForApplication Context context, GtasksListService gtasksListService,
                               GtasksPreferenceService gtasksPreferenceService, GtasksMetadata gtasksMetadata) {
        this.context = context;
        this.gtasksListService = gtasksListService;
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.gtasksMetadata = gtasksMetadata;
    }

    public List<Filter> getFilters() {
        // if we aren't logged in (or we are logged in to astrid.com), don't expose features
        if(!gtasksPreferenceService.isLoggedIn()) {
            return emptyList();
        }

        int cloud = R.drawable.ic_cloud_queue_24dp;

        List<Filter> listFilters = newArrayList();
        for (GtasksList list : gtasksListService.getLists()) {
            Filter filter = filterFromList(gtasksMetadata, context, list);
            filter.icon = cloud;
            listFilters.add(filter);
        }
        return listFilters;
    }

    public static Filter filterFromList(GtasksMetadata gtasksMetadata, Context context, GtasksList list) {
        String listName = list.getName();
        ContentValues values = new ContentValues();
        values.putAll(gtasksMetadata.createEmptyMetadata(AbstractModel.NO_ID).getMergedValues());
        values.remove(Metadata.TASK.name);
        values.put(GtasksMetadata.LIST_ID.name, list.getRemoteId());
        values.put(GtasksMetadata.ORDER.name, PermaSql.VALUE_NOW);
        FilterWithCustomIntent filter = new FilterWithCustomIntent(listName,
                new QueryTemplate().join(
                Join.left(Metadata.TABLE, Task.ID.eq(Metadata.TASK))).where(Criterion.and(
                        MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY),
                        TaskCriteria.notDeleted(),
                        GtasksMetadata.LIST_ID.eq(list.getRemoteId()))).orderBy(
                                Order.asc(Functions.cast(GtasksMetadata.ORDER, "LONG"))), //$NON-NLS-1$
                values);
        filter.customTaskList = new ComponentName(context, GtasksListFragment.class);
        Bundle extras = new Bundle();
        extras.putLong(GtasksListFragment.TOKEN_STORE_ID, list.getId());
        filter.customExtras = extras;

        return filter;
    }
}
