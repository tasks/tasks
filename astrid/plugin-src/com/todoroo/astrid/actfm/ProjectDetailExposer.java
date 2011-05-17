/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.api.AstridApiConstants;

/**
 * Exposes Task Detail for notes
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagDataDetailExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        String taskDetail = getTaskDetails(taskId);
        if(taskDetail == null)
            return;

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON,
                ActFmPreferenceService.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    public String getTaskDetails(long id) {
        /*Task task = PluginServices.getTaskService().fetchById(id, Task.PROJECT_ID);
        if(task == null)
            return null;
        TagData tagData = PluginServices.getTagDataService().fetchById(task.getValue(Task.PROJECT_ID), TagData.TITLE);
        if(tagData == null)*/
            return null;

        // return "<img src='silk_group'/> " + tagData.getValue(TagData.TITLE); //$NON-NLS-1$
    }

}
