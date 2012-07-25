/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.service.AstridDependencyInjector;

/**
 * Exposes Task Details for Google TAsks:
 * - list
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksDetailExposer extends BroadcastReceiver {

    public static final String DETAIL_SEPARATOR = " | "; //$NON-NLS-1$

    @Autowired private GtasksMetadataService gtasksMetadataService;
    @Autowired private GtasksListService gtasksListService;
    @Autowired private GtasksPreferenceService gtasksPreferenceService;

    static {
        AstridDependencyInjector.initialize();
    }

    public GtasksDetailExposer() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);

        // if we aren't logged in, don't expose features
        if(!gtasksPreferenceService.isLoggedIn())
            return;

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        String taskDetail = getTaskDetails(taskId);
        if(taskDetail == null)
            return;

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, GtasksPreferenceService.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    public String getTaskDetails(long id) {
        Metadata metadata = gtasksMetadataService.getTaskMetadata(id);
        if(metadata == null)
            return null;

        StringBuilder builder = new StringBuilder();

        String listId = metadata.getValue(GtasksMetadata.LIST_ID);
        if(listId == null || listId.equals(Preferences.getStringValue(GtasksPreferenceService.PREF_DEFAULT_LIST)))
            return null;
        String listName = gtasksListService.getListName(listId);
        if(listName == GtasksListService.LIST_NOT_FOUND)
            return null;

        builder.append("<img src='gtasks_detail'/> ").append(listName); //$NON-NLS-1$

        return builder.toString();
    }

}
