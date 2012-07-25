/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.todoroo.astrid.api.Addon;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;

public class NotesPlugin extends BroadcastReceiver {

    public static final String IDENTIFIER = "notes"; //$NON-NLS-1$

    @Override
    @SuppressWarnings("nls")
    public void onReceive(Context context, Intent intent) {
        Addon plugin = new Addon(IDENTIFIER, "Notes", "Todoroo",
                "Lets you add and view notes for a task.");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ADDONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    /**
     * Does this task have notes to display?
     *
     * @param task
     * @return
     */
    public static boolean hasNotes(Task task) {
        if(task.containsNonNullValue(Task.NOTES) && !TextUtils.isEmpty(task.getValue(Task.NOTES)))
            return true;

        if(PluginServices.getMetadataService().hasMetadata(task.getId(), NoteMetadata.METADATA_KEY))
            return true;

        return false;
    }

}
