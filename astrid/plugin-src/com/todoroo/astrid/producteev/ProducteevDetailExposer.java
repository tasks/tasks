/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.producteev.sync.ProducteevDashboard;
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
public class ProducteevDetailExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        boolean extended = intent.getBooleanExtra(AstridApiConstants.EXTRAS_EXTENDED, false);
        String taskDetail;
        try {
            taskDetail = getTaskDetails(context, taskId, extended);
        } catch (Exception e) {
            return;
        }
        if(taskDetail == null)
            return;

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, ProducteevUtilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_EXTENDED, extended);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    public String getTaskDetails(Context context, long id, boolean extended) {
        Metadata metadata = ProducteevDataService.getInstance().getTaskMetadata(id);
        if(metadata == null)
            return null;

        StringBuilder builder = new StringBuilder();

        // we always expose pdv notes. but, if we aren't logged in, don't expose other details
        if(!extended && !ProducteevUtilities.INSTANCE.isLoggedIn())
            return null;

        if(!extended) {
            long dashboardId = -1;
            if(metadata.containsNonNullValue(ProducteevTask.DASHBOARD_ID))
                dashboardId = metadata.getValue(ProducteevTask.DASHBOARD_ID);
            long responsibleId = -1;
            if(metadata.containsNonNullValue(ProducteevTask.RESPONSIBLE_ID))
                responsibleId = metadata.getValue(ProducteevTask.RESPONSIBLE_ID);
            long creatorId = -1;
            if(metadata.containsNonNullValue(ProducteevTask.CREATOR_ID))
                creatorId = metadata.getValue(ProducteevTask.CREATOR_ID);

            // display dashboard if not "no sync" or "default"
            StoreObject ownerDashboard = null;
            for(StoreObject dashboard : ProducteevDataService.getInstance().getDashboards()) {
                if(dashboard == null || !dashboard.containsNonNullValue(ProducteevDashboard.REMOTE_ID))
                    continue;

                if(dashboard.getValue(ProducteevDashboard.REMOTE_ID) == dashboardId) {
                    ownerDashboard = dashboard;
                    break;
                }
            }
            if(dashboardId != ProducteevUtilities.DASHBOARD_NO_SYNC && dashboardId
                    != Preferences.getLong(ProducteevUtilities.PREF_DEFAULT_DASHBOARD, 0L) &&
                    ownerDashboard != null) {
                String dashboardName = ownerDashboard.getValue(ProducteevDashboard.NAME);
                builder.append("<img src='silk_folder'/> ").append(dashboardName).append(TaskAdapter.DETAIL_SEPARATOR); //$NON-NLS-1$
            }

            // display responsible user if not current one
            if(responsibleId > 0 && ownerDashboard != null && responsibleId !=
                    Preferences.getLong(ProducteevUtilities.PREF_USER_ID, 0L)) {
                String user = getUserFromDashboard(ownerDashboard, responsibleId);
                if(user != null)
                    builder.append("<img src='silk_user_gray'/> ").append(user).append(TaskAdapter.DETAIL_SEPARATOR); //$NON-NLS-1$
            }

            // display creator user if not the current one
            if(creatorId > 0 && ownerDashboard != null && creatorId !=
                    Preferences.getLong(ProducteevUtilities.PREF_USER_ID, 0L)) {
                String user = getUserFromDashboard(ownerDashboard, creatorId);
                if(user != null)
                    builder.append("<img src='silk_user_orange'/> ").append( //$NON-NLS-1$
                            context.getString(R.string.producteev_PDE_task_from, user)).
                            append(TaskAdapter.DETAIL_SEPARATOR);
            }

        }

        if(Preferences.getBoolean(R.string.p_showNotes, false) == !extended) {
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

    /** Try and find user in the dashboard. return null if un-findable */
    private String getUserFromDashboard(StoreObject dashboard, long userId) {
        String users = ";" + dashboard.getValue(ProducteevDashboard.USERS); //$NON-NLS-1$
        int index = users.indexOf(";" + userId + ","); //$NON-NLS-1$ //$NON-NLS-2$
        if(index > -1)
            return users.substring(users.indexOf(',', index) + 1,
                    users.indexOf(';', index + 1));
        return null;
    }

}
