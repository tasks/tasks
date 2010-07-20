/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.DetailExposer;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.rmilk.data.MilkDataService;
import com.todoroo.astrid.rmilk.data.MilkNote;
import com.todoroo.astrid.rmilk.data.MilkTask;

/**
 * Exposes Task Details for Remember the Milk:
 * - RTM list
 * - RTM repeat information
 * - RTM notes
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MilkDetailExposer extends BroadcastReceiver implements DetailExposer{

    @Override
    public void onReceive(Context context, Intent intent) {
        // if we aren't logged in, don't expose features
        if(!Utilities.isLoggedIn())
            return;

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        boolean extended = intent.getBooleanExtra(AstridApiConstants.EXTRAS_EXTENDED, false);
        String taskDetail = getTaskDetails(context, taskId, extended);
        if(taskDetail == null)
            return;

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, Utilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_EXTENDED, extended);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    @Override
    public String getTaskDetails(Context context, long id, boolean extended) {
        Metadata metadata = MilkDataService.getInstance().getTaskMetadata(id);
        if(metadata == null)
            return null;

        StringBuilder builder = new StringBuilder();

        if(!extended) {
            long listId = metadata.getValue(MilkTask.LIST_ID);
            if(listId > 0) {
                builder.append(context.getString(R.string.rmilk_TLA_list,
                        MilkDataService.getInstance().getListName(listId))).append(TaskAdapter.DETAIL_SEPARATOR);
            }

            int repeat = metadata.getValue(MilkTask.REPEATING);
            if(repeat != 0) {
                builder.append(context.getString(R.string.rmilk_TLA_repeat)).append(TaskAdapter.DETAIL_SEPARATOR);
            }
        } else {
            TodorooCursor<Metadata> notesCursor = MilkDataService.getInstance().getTaskNotesCursor(id);
            try {
                for(notesCursor.moveToFirst(); !notesCursor.isAfterLast(); notesCursor.moveToNext()) {
                    metadata.readFromCursor(notesCursor);
                    builder.append(MilkNote.toTaskDetail(metadata)).append(TaskAdapter.DETAIL_SEPARATOR);
                }
            } finally {
                notesCursor.close();
            }
        }

        if(builder.length() == 0)
            return null;
        String result = builder.toString();
        return result.substring(0, result.length() - TaskAdapter.DETAIL_SEPARATOR.length());
    }

    @Override
    public String getPluginIdentifier() {
        return Utilities.IDENTIFIER;
    }

}
