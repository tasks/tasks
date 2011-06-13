/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes;

import android.text.TextUtils;
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
        if(task == null || TextUtils.isEmpty(task.getValue(Task.NOTES)))
            return null;


        TaskDecoration decoration;
        RemoteViews remoteViews = new RemoteViews(ContextManager.getContext().getPackageName(),
                R.layout.note_decoration);
        decoration = new TaskDecoration(remoteViews, TaskDecoration.POSITION_RIGHT, 0);

        return decoration;
    }

    @Override
    public String getAddon() {
        return NotesPlugin.IDENTIFIER;
    }

}
