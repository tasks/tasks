/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gcal;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

import timber.log.Timber;

public class GCalTaskCompleteListener extends InjectingBroadcastReceiver {

    @Inject TaskDao taskDao;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1) {
            return;
        }

        Task task = taskDao.fetch(taskId, Task.ID, Task.TITLE, Task.CALENDAR_URI);
        if(task == null) {
            return;
        }

        String calendarUri = task.getCalendarURI();
        if(!TextUtils.isEmpty(calendarUri)) {
            try {
                // change title of calendar event
                ContentResolver cr = context.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(CalendarContract.Events.TITLE, context.getString(R.string.gcal_completed_title,
                        task.getTitle()));
                cr.update(Uri.parse(calendarUri), values, null, null);
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
            }
        }
    }

    @Override
    protected void inject(BroadcastComponent component) {
        component.inject(this);
    }

}
