/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.DetailExposer;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.model.Task;

/**
 * Exposes Task Detail for notes
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class NoteDetailExposer extends BroadcastReceiver implements DetailExposer {

    @Override
    public void onReceive(Context context, Intent intent) {
        // get tags associated with this task
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        boolean extended = intent.getBooleanExtra(AstridApiConstants.EXTRAS_EXTENDED, false);
        String taskDetail = getTaskDetails(context, taskId, extended);
        if(taskDetail == null)
            return;

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, NotesPlugin.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_EXTENDED, extended);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    @Override
    public String getTaskDetails(Context context, long id, boolean extended) {
        if(!extended)
            return null;

        Task task = PluginServices.getTaskService().fetchById(id, Task.NOTES);
        if(task == null)
            return null;
        String notes = task.getValue(Task.NOTES);
        if(notes.length() == 0)
            return null;

        return notes;
    }

    @Override
    public String getPluginIdentifier() {
        return NotesPlugin.IDENTIFIER;
    }

}
