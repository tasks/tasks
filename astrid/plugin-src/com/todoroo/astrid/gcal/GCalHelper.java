package com.todoroo.astrid.gcal;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;

public class GCalHelper {
    public static void deleteTaskEvent(Task task) {
        String uri;
        if(task.containsNonNullValue(Task.CALENDAR_URI))
            uri = task.getValue(Task.CALENDAR_URI);
        else {
            task = PluginServices.getTaskService().fetchById(task.getId(), Task.CALENDAR_URI);
            if(task == null)
                return;
            uri = task.getValue(Task.CALENDAR_URI);
        }

        if(!TextUtils.isEmpty(uri)) {
            try {
                Uri calendarUri = Uri.parse(uri);

                // try to load calendar
                ContentResolver cr = ContextManager.getContext().getContentResolver();
                Cursor cursor = cr.query(calendarUri, new String[] { "dtstart" }, null, null, null); //$NON-NLS-1$
                boolean deleted = cursor.getCount() == 0;
                cursor.close();

                if (!deleted) {
                    cr.delete(calendarUri, null, null);
                }
            } catch (Exception e) {
                Log.e("astrid-gcal", "error-deleting-calendar-event", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }
}
