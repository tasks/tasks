package com.todoroo.astrid.gcal;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;

public class GCalHelper {
    public static void deleteTaskEvent(Task task) {
        String uri = task.getValue(Task.CALENDAR_URI);
        Uri calendarUri = null;

        if (TextUtils.isEmpty(uri)) {
            task = PluginServices.getTaskService().fetchById(task.getId(), Task.ID, Task.CALENDAR_URI);
            uri = task.getValue(Task.CALENDAR_URI);
        }

        if(!TextUtils.isEmpty(uri)) {
            try {
                calendarUri = Uri.parse(uri);

                // try to load calendar
                ContentResolver cr = ContextManager.getContext().getContentResolver();
                Cursor cursor = cr.query(calendarUri, new String[] { "dtstart" }, null, null, null); //$NON-NLS-1$
                boolean deleted = cursor.getCount() == 0;
                cursor.close();

                if (!deleted) {
                    cr.delete(calendarUri, null, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
