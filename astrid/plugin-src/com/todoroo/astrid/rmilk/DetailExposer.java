/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.R;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskDetail;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.rmilk.data.MilkDataService;

/**
 * Exposes {@link TaskDetail}s for Remember the Milk:
 * - RTM list
 * - RTM repeat information
 * - whether task has been changed
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DetailExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // if we aren't logged in, don't expose features
        if(!Utilities.isLoggedIn())
            return;

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        MilkDataService service = new MilkDataService(context);
        Task task = service.readTask(taskId);

        if(task == null)
            return;

        TaskDetail[] details = new TaskDetail[2];

        String listId = task.getValue(MilkDataService.LIST_ID);
        if(listId != null && listId.length() > 0)
            details[0] = new TaskDetail(context.getString(R.string.rmilk_TLA_list,
                    service.getList(listId)));
        else
            details[0] = null;

        int repeat = task.getValue(MilkDataService.REPEAT);
        if(repeat != 0)
            details[1] = new TaskDetail(context.getString(R.string.rmilk_TLA_repeat));
        else
            details[1] = null;

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ITEMS, details);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
