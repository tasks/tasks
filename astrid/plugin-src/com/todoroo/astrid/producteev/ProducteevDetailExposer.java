/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.producteev.sync.ProducteevDashboard;
import com.todoroo.astrid.producteev.sync.ProducteevDataService;
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
        ContextManager.setContext(context);
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        String taskDetail;
        try {
            taskDetail = getTaskDetails(context, taskId);
        } catch (Exception e) {
            return;
        }
        if(taskDetail == null)
            return;

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, ProducteevUtilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    @SuppressWarnings("nls")
    public String getTaskDetails(Context context, long id) {
        Metadata metadata = ProducteevDataService.getInstance().getTaskMetadata(id);
        if(metadata == null)
            return null;

        StringBuilder builder = new StringBuilder();

        if(!ProducteevUtilities.INSTANCE.isLoggedIn())
            return null;

        long dashboardId = -1;
        if(metadata.containsNonNullValue(ProducteevTask.DASHBOARD_ID))
            dashboardId = metadata.getValue(ProducteevTask.DASHBOARD_ID);
        long responsibleId = -1;
        if(metadata.containsNonNullValue(ProducteevTask.RESPONSIBLE_ID))
            responsibleId = metadata.getValue(ProducteevTask.RESPONSIBLE_ID);
        long creatorId = -1;
        if(metadata.containsNonNullValue(ProducteevTask.CREATOR_ID))
            creatorId = metadata.getValue(ProducteevTask.CREATOR_ID);
        String repeatSetting = null;
        if(metadata.containsNonNullValue(ProducteevTask.REPEATING_SETTING))
            repeatSetting = metadata.getValue(ProducteevTask.REPEATING_SETTING);

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
            String user = ProducteevDashboard.getUserFromDashboard(ownerDashboard, responsibleId);
            if(user != null)
                builder.append("<img src='silk_user_gray'/> ").append(user).append(TaskAdapter.DETAIL_SEPARATOR); //$NON-NLS-1$
        } else {
            // display creator user if not responsible user
            if(creatorId > 0 && ownerDashboard != null && creatorId != responsibleId) {
                String user = ProducteevDashboard.getUserFromDashboard(ownerDashboard, creatorId);
                if(user != null)
                    builder.append("<img src='silk_user_orange'/> ").append( //$NON-NLS-1$
                            context.getString(R.string.producteev_PDE_task_from, user)).
                            append(TaskAdapter.DETAIL_SEPARATOR);
            }
        }

        // display repeating task information
        if (repeatSetting != null && repeatSetting.length() > 0) {
            String interval = null;
            String[] pdvRepeating = repeatSetting.split(",");
            int pdvRepeatingValue = 0;
            String pdvRepeatingDay = null;
            try {
                pdvRepeatingValue = Integer.parseInt(pdvRepeating[0]);
            } catch (Exception e) {
                pdvRepeatingDay = pdvRepeating[0];
                pdvRepeatingValue = 1;
            }
            String pdvRepeatingInterval = pdvRepeating[1];

            if (pdvRepeatingInterval.startsWith("day")) {
                interval = context.getResources().getQuantityString(R.plurals.DUt_days, pdvRepeatingValue,
                        pdvRepeatingValue);
            } else if (pdvRepeatingInterval.startsWith("weekday")) {
                interval = context.getResources().getQuantityString(R.plurals.DUt_weekdays, pdvRepeatingValue,
                        pdvRepeatingValue);
            } else if (pdvRepeatingInterval.startsWith("week")) {
                interval = context.getResources().getQuantityString(R.plurals.DUt_weeks, pdvRepeatingValue,
                        pdvRepeatingValue);
            } else if (pdvRepeatingInterval.startsWith("month")) {
                interval = context.getResources().getQuantityString(R.plurals.DUt_months, pdvRepeatingValue,
                        pdvRepeatingValue);
            } else if (pdvRepeatingInterval.startsWith("year")) {
                interval = context.getResources().getQuantityString(R.plurals.DUt_years, pdvRepeatingValue,
                        pdvRepeatingValue);
            }
            interval = "<b>" + interval + "</b>";  //$NON-NLS-1$//$NON-NLS-2$
            if (pdvRepeatingDay != null) {
                DateFormatSymbols dfs = new DateFormatSymbols();
                String[] weekdays = dfs.getShortWeekdays();
                if (pdvRepeatingDay.equals("monday")) {
                    pdvRepeatingDay = weekdays[Calendar.MONDAY];
                } else if (pdvRepeatingDay.equals("tuesday")) {
                    pdvRepeatingDay = weekdays[Calendar.TUESDAY];
                } else if (pdvRepeatingDay.equals("wednesday")) {
                    pdvRepeatingDay = weekdays[Calendar.WEDNESDAY];
                } else if (pdvRepeatingDay.equals("thursday")) {
                    pdvRepeatingDay = weekdays[Calendar.THURSDAY];
                } else if (pdvRepeatingDay.equals("friday")) {
                    pdvRepeatingDay = weekdays[Calendar.FRIDAY];
                } else if (pdvRepeatingDay.equals("saturday")) {
                    pdvRepeatingDay = weekdays[Calendar.SATURDAY];
                } else if (pdvRepeatingDay.equals("sunday")) {
                    pdvRepeatingDay = weekdays[Calendar.SUNDAY];
                }
                interval = context.getResources().getString(R.string.repeat_detail_byday).replace("$I",  //$NON-NLS-1$
                        interval).replace("$D", pdvRepeatingDay); //$NON-NLS-1$
            }
            String detail = context.getString(R.string.repeat_detail_duedate, interval);
            builder.append("<img src='repeating_deadline'/> ").append(detail). //$NON-NLS-1$
                    append(TaskAdapter.DETAIL_SEPARATOR);
        }

        if(builder.length() == 0)
            return null;
        String result = builder.toString();
        return result.substring(0, result.length() - TaskAdapter.DETAIL_SEPARATOR.length());
    }

}
