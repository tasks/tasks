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

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;

/**
 * Exposes Task Detail for notes
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class NotesDetailExposer extends BroadcastReceiver {

    private static final int NOTE_MAX = 200;

    @Override
    public void onReceive(Context context, Intent intent) {
        // get tags associated with this task
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        String taskDetail = getTaskDetails(taskId);
        if(taskDetail == null)
            return;

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, NotesPlugin.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    @SuppressWarnings("nls")
    public String getTaskDetails(long id) {
        if(!Preferences.getBoolean(R.string.p_showNotes, false))
            return null;

        Task task = PluginServices.getTaskService().fetchById(id, Task.ID, Task.NOTES);
        if(task == null)
            return null;

        StringBuilder notesBuilder = new StringBuilder();

        String notes = task.getValue(Task.NOTES);
        if(!TextUtils.isEmpty(notes)) {
            if(notes.length() > NOTE_MAX) {
                int lastSpace = notes.lastIndexOf(' ', NOTE_MAX);
                notes = notes.substring(0, Math.max(lastSpace, NOTE_MAX - 20)) + "...";
            }
            notesBuilder.append(notes);
        }

        TodorooCursor<Metadata> cursor = PluginServices.getMetadataService().query(
                Query.select(Metadata.PROPERTIES).where(
                        MetadataCriteria.byTaskAndwithKey(task.getId(),
                                NoteMetadata.METADATA_KEY)).orderBy(Order.asc(Metadata.CREATION_DATE)));
        Metadata metadata = new Metadata();
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                metadata.readFromCursor(cursor);

                if(notesBuilder.length() > 0)
                    notesBuilder.append("\n");
                notesBuilder.append("<b>").append(metadata.getValue(NoteMetadata.TITLE)).append("</b>\n");
                notesBuilder.append(metadata.getValue(NoteMetadata.BODY));
            }
        } finally {
            cursor.close();
        }

        if(notesBuilder.length() == 0)
            return null;

        return "<img src='silk_note'/> " + notesBuilder; //$NON-NLS-1$
    }

}
