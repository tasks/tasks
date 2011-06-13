/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;

/**
 * Exposes {@link TaskDecoration} for timers
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TimerActionExposer extends BroadcastReceiver {

    private static final String TIMER_ACTION = "com.todoroo.astrid.TIMER_BUTTON"; //$NON-NLS-1$

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        Task task = PluginServices.getTaskService().fetchById(taskId, Task.ID, Task.TIMER_START,
                Task.ELAPSED_SECONDS);
        if(task == null)
            return;

        // was part of a broadcast for actions
        if(AstridApiConstants.BROADCAST_REQUEST_ACTIONS.equals(intent.getAction())) {
            final String label;
            final int drawable;
            if(task.getValue(Task.TIMER_START) == 0) {
                label = context.getString(R.string.TAE_startTimer);
                drawable = R.drawable.ic_qbar_timer_start;
            } else {
                label = context.getString(R.string.TAE_stopTimer);
                drawable = R.drawable.ic_qbar_timer_stop;
            }
            Intent newIntent = new Intent(TIMER_ACTION);
            newIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            TaskAction action = new TaskAction(label,
                    PendingIntent.getBroadcast(context, (int)taskId, newIntent, 0), null);
            action.drawable = drawable;

            // transmit
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ACTIONS);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, TimerPlugin.IDENTIFIER);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, action);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
        } else if(TIMER_ACTION.equals(intent.getAction())) {
            if(task.getValue(Task.TIMER_START) == 0)
                TimerPlugin.updateTimer(context, task, true);
            else
                TimerPlugin.updateTimer(context, task, false);
        }
    }

}
