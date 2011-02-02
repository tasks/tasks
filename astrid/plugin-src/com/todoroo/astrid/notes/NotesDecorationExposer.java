/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes;

import android.app.PendingIntent;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.api.TaskDecorationExposer;
import com.todoroo.astrid.data.Task;

/**
 * Exposes {@link TaskDecoration} for notes
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class NotesDecorationExposer implements TaskDecorationExposer {

    @Override
    public TaskDecoration expose(Task task) {
        if(task == null || TextUtils.isEmpty(task.getValue(Task.NOTES)))
            return null;

        TaskDecoration decoration;
        RemoteViews remoteViews = new RemoteViews(ContextManager.getContext().getPackageName(),
                R.layout.note_decoration);
        decoration = new TaskDecoration(remoteViews, TaskDecoration.POSITION_RIGHT, 0);

        Intent intent = new Intent(ContextManager.getContext(), NoteViewingActivity.class);
        intent.putExtra(NoteViewingActivity.EXTRA_TASK, task);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(ContextManager.getContext(),
                (int)task.getId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);

        remoteViews.setOnClickPendingIntent(R.id.icon, pendingIntent);

        return decoration;
    }

    @Override
    public String getAddon() {
        return NotesPlugin.IDENTIFIER;
    }

}
