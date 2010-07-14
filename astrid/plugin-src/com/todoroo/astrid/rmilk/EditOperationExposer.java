/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.R;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.EditOperation;

/**
 * Exposes {@link EditOperation} for Remember the Milk
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class EditOperationExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // if we aren't logged in, don't expose features
        if(!Utilities.isLoggedIn())
            return;

        long taskId = intent
                .getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if (taskId == -1)
            return;

        EditOperation taskEditOperation;
        Intent editIntent = new Intent(context, MilkEditActivity.class);
        taskEditOperation = new EditOperation(context.getString(
                R.string.rmilk_EOE_button), editIntent);

        // transmit
        EditOperation[] operations = new EditOperation[1];
        operations[0] = taskEditOperation;
        Intent broadcastIntent = new Intent(
                AstridApiConstants.BROADCAST_SEND_EDIT_OPERATIONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ITEMS, operations);
        context.sendBroadcast(broadcastIntent,
                AstridApiConstants.PERMISSION_READ);
    }

}
