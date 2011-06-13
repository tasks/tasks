/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.api.TaskDecoration;

/**
 * Exposes {@link TaskDecoration} for timers
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class EditPeopleExposer extends BroadcastReceiver {

    private static final String ACTION = "com.todoroo.astrid.EDIT_PEOPLE"; //$NON-NLS-1$

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        if(AstridApiConstants.BROADCAST_REQUEST_ACTIONS.equals(intent.getAction())) {
            final String label = context.getString(R.string.EPE_action);
            final int drawable = R.drawable.ic_qbar_share;
            Intent newIntent = new Intent(ACTION);
            newIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            TaskAction action = new TaskAction(label,
                    PendingIntent.getBroadcast(context, (int)taskId, newIntent, 0), null);
            action.drawable = drawable;

            // transmit
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ACTIONS);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, ActFmPreferenceService.IDENTIFIER);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, action);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
        } else if(ACTION.equals(intent.getAction())) {
            Intent launchIntent = new Intent(context, EditPeopleActivity.class);
            launchIntent.putExtra(EditPeopleActivity.EXTRA_TASK_ID, taskId);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ContextManager.getContext().startActivity(launchIntent);
        }
    }

}
