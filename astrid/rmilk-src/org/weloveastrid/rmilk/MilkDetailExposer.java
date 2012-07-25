/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.rmilk;

import org.weloveastrid.rmilk.data.MilkListService;
import org.weloveastrid.rmilk.data.MilkMetadataService;
import org.weloveastrid.rmilk.data.MilkTaskFields;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;

/**
 * Exposes Task Details for Remember the Milk:
 * - RTM list
 * - RTM repeat information
 * - RTM notes
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MilkDetailExposer extends BroadcastReceiver {

    public static final String DETAIL_SEPARATOR = " | "; //$NON-NLS-1$

    @Autowired private MilkMetadataService milkMetadataService;
    @Autowired private MilkListService milkListService;

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        MilkDependencyInjector.initialize();
        DependencyInjectionService.getInstance().inject(this);

        // if we aren't logged in, don't expose features
        if(!MilkUtilities.INSTANCE.isLoggedIn())
            return;

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        String taskDetail = getTaskDetails(context, taskId);
        if(taskDetail == null)
            return;

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, MilkUtilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    public String getTaskDetails(Context context, long id) {
        Metadata metadata = milkMetadataService.getTaskMetadata(id);
        if(metadata == null)
            return null;

        StringBuilder builder = new StringBuilder();

        long listId = metadata.getValue(MilkTaskFields.LIST_ID);
        String listName = milkListService.getListName(listId);
        // RTM list is out of date. don't display RTM stuff
        if(listName == null)
            return null;

        if(listId > 0 && !"Inbox".equals(listName)) { //$NON-NLS-1$
            builder.append("<img src='silk_folder'/> ").append(listName).append(DETAIL_SEPARATOR); //$NON-NLS-1$
        }

        int repeat = metadata.getValue(MilkTaskFields.REPEATING);
        if(repeat != 0) {
            builder.append(context.getString(R.string.rmilk_TLA_repeat)).append(DETAIL_SEPARATOR);
        }

        if(builder.length() == 0)
            return null;
        String result = builder.toString();
        return result.substring(0, result.length() - DETAIL_SEPARATOR.length());
    }

}
