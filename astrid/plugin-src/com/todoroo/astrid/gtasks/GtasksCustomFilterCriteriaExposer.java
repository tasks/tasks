/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.CustomFilterCriterion;
import com.todoroo.astrid.api.MultipleSelectCriterion;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;

public class GtasksCustomFilterCriteriaExposer extends BroadcastReceiver {
    @Autowired private GtasksPreferenceService gtasksPreferenceService;
    @Autowired private GtasksListService gtasksListService;

    private static final String IDENTIFIER = "gtaskslist"; //$NON-NLS-1$

    static {
        AstridDependencyInjector.initialize();
    }

    @SuppressWarnings("nls")
    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        DependencyInjectionService.getInstance().inject(this);

        // if we aren't logged in, don't expose sync action
        if(!gtasksPreferenceService.isLoggedIn())
            return;

        Resources r = context.getResources();


        StoreObject[] lists = gtasksListService.getLists();

        CustomFilterCriterion[] result = new CustomFilterCriterion[1];
        String[] listNames = new String[lists.length];
        String[] listIds = new String[lists.length];
        for (int i = 0; i < lists.length; i++) {
            listNames[i] = lists[i].getValue(GtasksList.NAME);
            listIds[i] = lists[i].getValue(GtasksList.REMOTE_ID);
        }

        ContentValues values = new ContentValues();
        values.putAll(GtasksMetadata.createEmptyMetadata(AbstractModel.NO_ID).getMergedValues());
        values.remove(Metadata.TASK.name);
        values.put(GtasksMetadata.LIST_ID.name, "?");

        CustomFilterCriterion criterion = new MultipleSelectCriterion(
                IDENTIFIER,
                context.getString(R.string.CFC_gtasks_list_text),

                Query.select(Metadata.TASK).from(Metadata.TABLE).join(Join.inner(
                        Task.TABLE, Metadata.TASK.eq(Task.ID))).where(Criterion.and(
                        TaskDao.TaskCriteria.activeAndVisible(),
                        MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY),
                        GtasksMetadata.LIST_ID.eq("?"))).toString(),

                values,
                listNames,
                listIds,
                ((BitmapDrawable)r.getDrawable(R.drawable.gtasks_icon)).getBitmap(),
                context.getString(R.string.CFC_gtasks_list_name));
        result[0] = criterion;

        // transmit filter list
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_CUSTOM_FILTER_CRITERIA);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, GtasksPreferenceService.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, result);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }
}
