/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gcal;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;

public class GCalTaskCompleteListener extends BroadcastReceiver {

    @SuppressWarnings("nls")
    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        Task task = PluginServices.getTaskService().fetchById(taskId, Task.ID, Task.TITLE, Task.CALENDAR_URI);
        if(task == null)
            return;

        String calendarUri = task.getValue(Task.CALENDAR_URI);
        if(!TextUtils.isEmpty(calendarUri)) {
            try {
                // change title of calendar event
                ContentResolver cr = context.getContentResolver();
                ContentValues values = new ContentValues();
                values.put("title", context.getString(R.string.gcal_completed_title,
                        task.getValue(Task.TITLE)));
                cr.update(Uri.parse(calendarUri), values, null, null);
            } catch (Exception e) {
                Log.d("astrid-gcal", "Error updating calendar entry", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

}
