/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.DetailExposer;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.producteev.sync.ProducteevDataService;
import com.todoroo.astrid.producteev.sync.ProducteevNote;
import com.todoroo.astrid.producteev.sync.ProducteevTask;

/**
 * Exposes Task Details for Producteev:
 * - notes
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ProducteevDetailExposer extends BroadcastReceiver implements DetailExposer{

    @Override
    public void onReceive(Context context, Intent intent) {
        // if we aren't logged in, don't expose features
        if(!ProducteevUtilities.INSTANCE.isLoggedIn())
            return;

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        boolean extended = intent.getBooleanExtra(AstridApiConstants.EXTRAS_EXTENDED, false);
        String taskDetail = getTaskDetails(context, taskId, extended);
        if(taskDetail == null)
            return;

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, ProducteevUtilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_EXTENDED, extended);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    @Override
    public String getTaskDetails(Context context, long id, boolean extended) {
        Metadata metadata = ProducteevDataService.getInstance().getTaskMetadata(id);
        if(metadata == null)
            return null;

        StringBuilder builder = new StringBuilder();

        if(!extended) {
            long dashboardId = metadata.getValue(ProducteevTask.DASHBOARD_ID);
            String dashboardName = ProducteevDataService.getInstance().getDashboardName(dashboardId);
            // Prod dashboard is out of date. don't display Producteev stuff
            if(dashboardName == null)
                return null;

            if(dashboardId > 0) {
                builder.append(context.getString(R.string.producteev_TLA_dashboard,
                        dashboardId)).append(TaskAdapter.DETAIL_SEPARATOR);
            }

        } else {
        TodorooCursor<Metadata> notesCursor = ProducteevDataService.getInstance().getTaskNotesCursor(id);
        try {
            for(notesCursor.moveToFirst(); !notesCursor.isAfterLast(); notesCursor.moveToNext()) {
                metadata.readFromCursor(notesCursor);
                builder.append(metadata.getValue(ProducteevNote.MESSAGE)).append(TaskAdapter.DETAIL_SEPARATOR);
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
        return ProducteevUtilities.IDENTIFIER;
    }

}