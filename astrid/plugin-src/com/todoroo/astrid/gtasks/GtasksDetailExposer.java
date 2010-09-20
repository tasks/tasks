/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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
public class GtasksDetailExposer extends BroadcastReceiver {

    public static final String DETAIL_SEPARATOR = " | "; //$NON-NLS-1$

    @Autowired private GtasksMetadataService gtasksMetadataService;
    @Autowired private GtasksListService gtasksListService;
    @Autowired private GtasksPreferenceService gtasksPreferenceService;

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);

        // if we aren't logged in, don't expose features
        if(!gtasksPreferenceService.isLoggedIn())
            return;

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        boolean extended = intent.getBooleanExtra(AstridApiConstants.EXTRAS_EXTENDED, false);
        String taskDetail = getTaskDetails(taskId, extended);
        if(taskDetail == null)
            return;

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, GtasksPreferenceService.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_EXTENDED, extended);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    public String getTaskDetails(long id, boolean extended) {
        if(extended)
            return null;

        DependencyInjectionService.getInstance().inject(this);

        Metadata metadata = gtasksMetadataService.getTaskMetadata(id);
        if(metadata == null)
            return null;

        StringBuilder builder = new StringBuilder();

        long listId = metadata.getValue(GtasksMetadata.LIST_ID);
        String listName = gtasksListService.getListName(listId);
        // RTM list is out of date. don't display RTM stuff
        if(listName == GtasksListService.LIST_NOT_FOUND)
            return null;

        builder.append("<img src='silk_folder'/> ").append(listName); //$NON-NLS-1$

        return builder.toString();
    }

}
