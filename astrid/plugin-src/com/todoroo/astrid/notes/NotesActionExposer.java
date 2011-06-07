/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

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
public class NotesActionExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        Task task = PluginServices.getTaskService().fetchById(taskId, Task.ID, Task.TITLE, Task.NOTES);
        if(!task.containsNonNullValue(Task.NOTES) || TextUtils.isEmpty(task.getValue(Task.NOTES)))
            return;

        if(AstridApiConstants.BROADCAST_REQUEST_ACTIONS.equals(intent.getAction())) {
            sendNoteAction(context, taskId);
        } else {
            displayNote(context, task);
        }
    }

    private void displayNote(Context context, Task task) {
        Intent intent = new Intent(context, EditNoteActivity.class);
        intent.putExtra(EditNoteActivity.EXTRA_TASK_ID, task);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private void sendNoteAction(Context context, long taskId) {
        final String label = context.getString(R.string.TEA_note_label);
        final Drawable drawable = context.getResources().getDrawable(R.drawable.tango_notes);

        Bitmap icon = ((BitmapDrawable)drawable).getBitmap();
        Intent newIntent = new Intent(context, getClass());
        newIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        TaskAction action = new TaskAction(label,
                PendingIntent.getBroadcast(context, (int)taskId, newIntent, 0), icon);

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ACTIONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, NotesPlugin.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, action);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
