/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;

import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

/**
 * Exposes Task Details for Google TAsks:
 * - list
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksDetailExposer extends InjectingBroadcastReceiver {

    @Inject GtasksMetadataService gtasksMetadataService;
    @Inject GtasksListService gtasksListService;
    @Inject GtasksPreferenceService gtasksPreferenceService;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        ContextManager.setContext(context);

        // if we aren't logged in, don't expose features
        if(!gtasksPreferenceService.isLoggedIn()) {
            return;
        }

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1) {
            return;
        }

        String taskDetail = getTaskDetails(taskId);
        if(taskDetail == null) {
            return;
        }

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, GtasksPreferenceService.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    private String getTaskDetails(long id) {
        Metadata metadata = gtasksMetadataService.getTaskMetadata(id);
        if(metadata == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        String listId = metadata.getValue(GtasksMetadata.LIST_ID);
        if(listId == null || listId.equals(gtasksPreferenceService.getDefaultList())) {
            return null;
        }
        String listName = gtasksListService.getListName(listId);
        if(listName == null) {
            return null;
        }

        builder.append("<img src='gtasks_detail'/> ").append(listName); //$NON-NLS-1$

        return builder.toString();
    }

}
