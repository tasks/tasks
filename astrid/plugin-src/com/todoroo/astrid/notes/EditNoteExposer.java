/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
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
public class EditNoteExposer extends BroadcastReceiver {

    private static final String ACTION = "com.todoroo.astrid.EDIT_NOTES"; //$NON-NLS-1$

    @Autowired ActFmPreferenceService actFmPreferenceService;

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        DependencyInjectionService.getInstance().inject(this);
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        if(AstridApiConstants.BROADCAST_REQUEST_ACTIONS.equals(intent.getAction())) {
            String label;
            int drawable;

            if(!actFmPreferenceService.isLoggedIn()) {
                Task task = PluginServices.getTaskService().fetchById(taskId, Task.NOTES);
                if(task == null || TextUtils.isEmpty(task.getValue(Task.NOTES)))
                    return;
                label = context.getString(R.string.ENE_label);
                drawable = R.drawable.ic_qbar_comments;
            } else {
                label = context.getString(R.string.ENE_label_comments);
                drawable = R.drawable.ic_qbar_comments;
            }
            Intent newIntent = new Intent(ACTION);
            newIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            TaskAction action = new TaskAction(label,
                    PendingIntent.getBroadcast(context, (int)taskId, newIntent, 0), null);
            action.drawable = drawable;

            // transmit
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ACTIONS);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, NotesPlugin.IDENTIFIER);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, action);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
        } else if(ACTION.equals(intent.getAction())) {
            Intent launchIntent = new Intent(context, EditNoteActivity.class);
            launchIntent.putExtra(EditNoteActivity.EXTRA_TASK_ID, taskId);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ContextManager.getContext().startActivity(launchIntent);
        }
    }

}
