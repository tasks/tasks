/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes;

import android.app.PendingIntent;
import android.content.Intent;
import android.widget.RemoteViews;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.api.TaskDecorationExposer;
import com.todoroo.astrid.data.Task;

/**
 * Exposes {@link TaskDecoration} for timers
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class NotesDecorationExposer implements TaskDecorationExposer {

    @Override
    public TaskDecoration expose(Task task) {
        if(Preferences.getBoolean(R.string.p_showNotes, false))
            return null;
        if(task == null || !NotesPlugin.hasNotes(task))
            return null;

        Intent intent = new Intent(ContextManager.getContext(), EditNoteActivity.class);
        intent.setAction(EditNoteActivity.class.getName());
        intent.putExtra(EditNoteActivity.EXTRA_TASK_ID, task.getId());
        PendingIntent pi  = PendingIntent.getActivity(ContextManager.getContext(),
                (int)task.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        TaskDecoration decoration;
        RemoteViews remoteViews = new RemoteViews(ContextManager.getContext().getPackageName(),
                R.layout.note_decoration);
        remoteViews.setOnClickPendingIntent(R.id.icon, pi);

        decoration = new TaskDecoration(remoteViews, TaskDecoration.POSITION_RIGHT, 0);

        return decoration;
    }

    @Override
    public String getAddon() {
        return NotesPlugin.IDENTIFIER;
    }

}
