/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskDetail;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.rmilk.data.MilkDataService;
import com.todoroo.astrid.rmilk.data.MilkTask;

/**
 * Exposes {@link TaskDetail}s for Remember the Milk:
 * - RTM list
 * - RTM repeat information
 * - whether task has been changed
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MilkDetailExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // if we aren't logged in, don't expose features
        if(!Utilities.isLoggedIn())
            return;

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        Metadata metadata = MilkDataService.getInstance().getTaskMetadata(taskId);
        if(metadata == null)
            return;

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, Utilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);

        long listId = metadata.getValue(MilkTask.LIST_ID);
        if(listId > 0) {
            TaskDetail detail = new TaskDetail(context.getString(R.string.rmilk_TLA_list,
                    MilkDataService.getInstance().getListName(listId)));
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, detail);
            context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
        }

        int repeat = metadata.getValue(MilkTask.REPEATING);
        if(repeat != 0) {
            TaskDetail detail = new TaskDetail(context.getString(R.string.rmilk_TLA_repeat));
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, detail);
            context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
        }
    }

}
