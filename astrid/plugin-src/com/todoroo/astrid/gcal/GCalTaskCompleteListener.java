package com.todoroo.astrid.gcal;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.TaskService;

public class GCalTaskCompleteListener extends BroadcastReceiver {

    @Autowired
    private TaskService taskService;

    @SuppressWarnings("nls")
    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        DependencyInjectionService.getInstance().inject(this);

        Task task = taskService.fetchById(taskId, Task.ID, Task.TITLE, Task.CALENDAR_URI);
        if(task == null)
            return;

        String calendarUri = task.getValue(Task.CALENDAR_URI);
        if(!TextUtils.isEmpty(calendarUri)) {
            // change title of calendar event
            ContentResolver cr = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put("title", context.getString(R.string.gcal_completed_title,
                    task.getValue(Task.TITLE)));
            cr.update(Uri.parse(calendarUri), values, null, null);
        }
    }

}
